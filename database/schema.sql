--
-- PostgreSQL database dump
--

\restrict 9IX04ZpS0nddEhgKd4iJCf3M8oygi3z4YZ77xmucSuZYXWkj6adk4eYP0cm9A7g

-- Dumped from database version 16.15
-- Dumped by pg_dump version 16.15

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


--
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';


--
-- Name: dano_severidad; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.dano_severidad AS ENUM (
    'LEVE',
    'MODERADO',
    'GRAVE',
    'TOTAL'
);


--
-- Name: factura_tipo; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.factura_tipo AS ENUM (
    'FACTURA',
    'NOTA_CREDITO'
);


--
-- Name: herramienta_pub_estado; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.herramienta_pub_estado AS ENUM (
    'PUBLICADA',
    'PAUSADA',
    'RETIRADA'
);


--
-- Name: pago_estado; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.pago_estado AS ENUM (
    'PENDIENTE',
    'COMPLETADO',
    'FALLIDO',
    'REEMBOLSADO'
);


--
-- Name: reserva_estado; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.reserva_estado AS ENUM (
    'PENDIENTE',
    'CONFIRMADA',
    'EN_CURSO',
    'FINALIZADA',
    'CANCELADA',
    'EXPIRADA'
);


--
-- Name: user_rol; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.user_rol AS ENUM (
    'ADMIN',
    'PROPIETARIO',
    'CLIENTE'
);


--
-- Name: calcular_disponibilidad_herramienta(uuid, date, date, uuid); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.calcular_disponibilidad_herramienta(p_herramienta_id uuid, p_fecha_inicio date, p_fecha_fin date, p_excluir_reserva_id uuid DEFAULT NULL::uuid) RETURNS integer
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_total INT;
    v_ocupacion_max INT := 0;
    v_dia DATE;
    v_ocupacion_dia INT;
BEGIN
    SELECT cantidad_total INTO v_total FROM herramienta WHERE id = p_herramienta_id;
    
    v_dia := p_fecha_inicio;
    WHILE v_dia <= p_fecha_fin LOOP
        SELECT COALESCE(SUM(dr.cantidad), 0) INTO v_ocupacion_dia
        FROM detalle_reserva dr
        JOIN reserva r ON r.id = dr.reserva_id
        WHERE dr.herramienta_id = p_herramienta_id
          AND (p_excluir_reserva_id IS NULL OR dr.reserva_id <> p_excluir_reserva_id)
          AND r.estado IN ('PENDIENTE', 'CONFIRMADA', 'EN_CURSO')
          AND (r.estado != 'PENDIENTE' 
               OR r.fecha_expiracion_pendiente IS NULL 
               OR r.fecha_expiracion_pendiente > clock_timestamp())
          AND r.fecha_inicio <= v_dia
          AND r.fecha_fin >= v_dia;
        
        IF v_ocupacion_dia > v_ocupacion_max THEN
            v_ocupacion_max := v_ocupacion_dia;
        END IF;
        
        v_dia := v_dia + INTERVAL '1 day';
    END LOOP;
    
    RETURN GREATEST(v_total - v_ocupacion_max, 0);
END;
$$;


--
-- Name: dias_inclusivos(date, date); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.dias_inclusivos(p_inicio date, p_fin date) RETURNS integer
    LANGUAGE plpgsql IMMUTABLE
    AS $$
BEGIN
    RETURN (p_fin - p_inicio) + 1;
END;
$$;


--
-- Name: generar_numero_factura(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.generar_numero_factura() RETURNS character varying
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_prefijo VARCHAR := (SELECT valor FROM configuracion_fiscal WHERE clave = 'factura_serie_prefijo');
    v_num BIGINT;
BEGIN
    v_num := nextval('factura_seq');
    RETURN v_prefijo || '-' || lpad(v_num::text, 6, '0');
END;
$$;


--
-- Name: prevent_detalle_reserva_modification(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.prevent_detalle_reserva_modification() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        IF OLD.precio_dia_acordado IS DISTINCT FROM NEW.precio_dia_acordado THEN
            RAISE EXCEPTION 'precio_dia_acordado es inmutable';
        END IF;
        IF OLD.cantidad IS DISTINCT FROM NEW.cantidad THEN
            RAISE EXCEPTION 'cantidad es inmutable; cancele y cree nueva reserva';
        END IF;
        IF OLD.herramienta_id IS DISTINCT FROM NEW.herramienta_id THEN
            RAISE EXCEPTION 'herramienta_id es inmutable';
        END IF;
        IF OLD.reserva_id IS DISTINCT FROM NEW.reserva_id THEN
            RAISE EXCEPTION 'reserva_id es inmutable';
        END IF;
        IF OLD.subtotal IS DISTINCT FROM NEW.subtotal
           OR OLD.iva IS DISTINCT FROM NEW.iva
           OR OLD.total IS DISTINCT FROM NEW.total
           OR OLD.iva_porcentaje IS DISTINCT FROM NEW.iva_porcentaje
           OR OLD.iva_aplica IS DISTINCT FROM NEW.iva_aplica THEN
            RAISE EXCEPTION 'valores calculados y tratamiento fiscal son inmutables';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;


--
-- Name: prevent_pago_completado_change(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.prevent_pago_completado_change() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF TG_OP = 'UPDATE' AND OLD.estado = 'COMPLETADO' THEN
        IF OLD.estado IS DISTINCT FROM NEW.estado
           OR OLD.monto IS DISTINCT FROM NEW.monto
           OR OLD.moneda IS DISTINCT FROM NEW.moneda
           OR OLD.reserva_id IS DISTINCT FROM NEW.reserva_id
           OR OLD.pago_original_id IS DISTINCT FROM NEW.pago_original_id
           OR OLD.transaccion_id IS DISTINCT FROM NEW.transaccion_id THEN
            RAISE EXCEPTION 'Pago COMPLETADO es inmutable en sus campos financieros e identidad';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;


--
-- Name: prevent_reserva_fechas_change(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.prevent_reserva_fechas_change() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        IF OLD.fecha_inicio IS DISTINCT FROM NEW.fecha_inicio 
           OR OLD.fecha_fin IS DISTINCT FROM NEW.fecha_fin THEN
            IF EXISTS (SELECT 1 FROM detalle_reserva WHERE reserva_id = OLD.id) THEN
                RAISE EXCEPTION 
                    'No se pueden cambiar fechas de una reserva con detalles. Cancele y cree nueva.';
            END IF;
        END IF;
    END IF;
    RETURN NEW;
END;
$$;


--
-- Name: proteger_reembolso_referenciado(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.proteger_reembolso_referenciado() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF TG_OP = 'UPDATE' AND OLD.estado = 'REEMBOLSADO' THEN
        IF EXISTS (SELECT 1 FROM factura WHERE pago_reembolso_id = OLD.id) THEN
            IF OLD.estado IS DISTINCT FROM NEW.estado
               OR OLD.monto IS DISTINCT FROM NEW.monto
               OR OLD.moneda IS DISTINCT FROM NEW.moneda
               OR OLD.reserva_id IS DISTINCT FROM NEW.reserva_id
               OR OLD.pago_original_id IS DISTINCT FROM NEW.pago_original_id THEN
                RAISE EXCEPTION 
                    'No se puede modificar un reembolso referenciado por una nota de crédito';
            END IF;
        END IF;
    END IF;
    RETURN NEW;
END;
$$;


--
-- Name: recalcular_cantidad_disponible_herramienta(uuid); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.recalcular_cantidad_disponible_herramienta(p_herramienta_id uuid) RETURNS void
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_total INT;
    v_ocupadas INT;
BEGIN
    SELECT cantidad_total INTO v_total FROM herramienta WHERE id = p_herramienta_id;
    IF v_total IS NULL THEN
        RETURN;
    END IF;

    SELECT COALESCE(SUM(dr.cantidad), 0) INTO v_ocupadas
    FROM detalle_reserva dr
    JOIN reserva r ON r.id = dr.reserva_id
    WHERE dr.herramienta_id = p_herramienta_id
      AND r.estado IN ('PENDIENTE', 'CONFIRMADA', 'EN_CURSO')
      AND (r.estado != 'PENDIENTE' OR r.fecha_expiracion_pendiente IS NULL OR r.fecha_expiracion_pendiente > clock_timestamp());

    UPDATE herramienta
    SET cantidad_disponible = GREATEST(0, v_total - v_ocupadas)
    WHERE id = p_herramienta_id;
END;
$$;


--
-- Name: trg_actualizar_disponibilidad_desde_detalle(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.trg_actualizar_disponibilidad_desde_detalle() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        PERFORM recalcular_cantidad_disponible_herramienta(OLD.herramienta_id);
        RETURN OLD;
    ELSE
        PERFORM recalcular_cantidad_disponible_herramienta(NEW.herramienta_id);
        IF TG_OP = 'UPDATE' AND OLD.herramienta_id <> NEW.herramienta_id THEN
            PERFORM recalcular_cantidad_disponible_herramienta(OLD.herramienta_id);
        END IF;
        RETURN NEW;
    END IF;
END;
$$;


--
-- Name: trg_actualizar_disponibilidad_desde_reserva(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.trg_actualizar_disponibilidad_desde_reserva() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN SELECT DISTINCT herramienta_id FROM detalle_reserva WHERE reserva_id = NEW.id LOOP
        PERFORM recalcular_cantidad_disponible_herramienta(r.herramienta_id);
    END LOOP;
    RETURN NEW;
END;
$$;


--
-- Name: trigger_actualizado_en(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.trigger_actualizado_en() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.actualizado_en = now();
    RETURN NEW;
END;
$$;


--
-- Name: validar_expiracion_pendiente(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.validar_expiracion_pendiente() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        IF NEW.estado = 'PENDIENTE' THEN
            IF NEW.fecha_expiracion_pendiente IS NULL THEN
                RAISE EXCEPTION 'Reserva PENDIENTE requiere fecha_expiracion_pendiente';
            END IF;
            IF NEW.fecha_expiracion_pendiente <= clock_timestamp() THEN
                RAISE EXCEPTION 'fecha_expiracion_pendiente debe ser futura';
            END IF;
        END IF;
    END IF;
    RETURN NEW;
END;
$$;


--
-- Name: validar_nota_credito_original(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.validar_nota_credito_original() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF TG_OP IN ('INSERT', 'UPDATE') AND NEW.tipo = 'NOTA_CREDITO' THEN
        IF NEW.factura_original_id IS NOT NULL THEN
            IF NOT EXISTS (
                SELECT 1 FROM factura 
                WHERE id = NEW.factura_original_id 
                  AND reserva_id = NEW.reserva_id 
                  AND tipo = 'FACTURA'
            ) THEN
                RAISE EXCEPTION 
                    'factura_original_id debe referenciar una FACTURA de la misma reserva';
            END IF;
        END IF;
    END IF;

    IF TG_OP = 'UPDATE' AND OLD.tipo = 'FACTURA' AND NEW.tipo = 'NOTA_CREDITO' THEN
        IF EXISTS (SELECT 1 FROM factura WHERE factura_original_id = OLD.id) THEN
            RAISE EXCEPTION 
                'No se puede convertir a NOTA_CREDITO una factura original referenciada por notas de crédito';
        END IF;
    END IF;

    RETURN NEW;
END;
$$;


--
-- Name: validar_nota_credito_reembolso_factura(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.validar_nota_credito_reembolso_factura() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF TG_OP IN ('INSERT', 'UPDATE') AND NEW.tipo = 'NOTA_CREDITO' THEN
        IF NEW.pago_reembolso_id IS NULL THEN
            RAISE EXCEPTION 'NOTA_CREDITO requiere pago_reembolso_id';
        END IF;
        IF NOT EXISTS (
            SELECT 1 FROM pago 
            WHERE id = NEW.pago_reembolso_id 
              AND estado = 'REEMBOLSADO' 
              AND reserva_id = NEW.reserva_id
        ) THEN
            RAISE EXCEPTION 
                'pago_reembolso_id debe referenciar un REEMBOLSADO de la misma reserva';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;


--
-- Name: verificar_subtotal_detalle(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.verificar_subtotal_detalle() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_dias INT;
    v_subtotal_esperado NUMERIC(14,2);
    v_iva_esperado NUMERIC(14,2);
    v_total_esperado NUMERIC(14,2);
BEGIN
    SELECT dias_inclusivos(r.fecha_inicio, r.fecha_fin) INTO v_dias
    FROM reserva r WHERE r.id = NEW.reserva_id;

    v_subtotal_esperado := NEW.precio_dia_acordado * NEW.cantidad * v_dias;
    
    IF NEW.iva_aplica THEN
        v_iva_esperado := round(v_subtotal_esperado * NEW.iva_porcentaje / 100, 2);
    ELSE
        v_iva_esperado := 0;
    END IF;
    v_total_esperado := v_subtotal_esperado + v_iva_esperado;

    IF NEW.subtotal != v_subtotal_esperado
       OR NEW.iva != v_iva_esperado
       OR NEW.total != v_total_esperado THEN
        RAISE EXCEPTION 
            'Valores incoherentes: subtotal=%, iva=%, total=% (esperado %, %, %)',
            NEW.subtotal, NEW.iva, NEW.total,
            v_subtotal_esperado, v_iva_esperado, v_total_esperado;
    END IF;
    RETURN NEW;
END;
$$;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: categoria; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.categoria (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    nombre character varying(100) NOT NULL,
    descripcion text,
    creado_en timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: cliente; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cliente (
    usuario_id uuid NOT NULL,
    direccion_entrega_default text,
    datos_facturacion jsonb
);


--
-- Name: configuracion_fiscal; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.configuracion_fiscal (
    clave character varying(50) NOT NULL,
    valor character varying(100) NOT NULL,
    descripcion text,
    actualizado_en timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: detalle_reserva; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.detalle_reserva (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    reserva_id uuid NOT NULL,
    herramienta_id uuid NOT NULL,
    cantidad integer NOT NULL,
    precio_dia_acordado numeric(14,2) NOT NULL,
    subtotal numeric(14,2) NOT NULL,
    iva numeric(14,2) NOT NULL,
    total numeric(14,2) NOT NULL,
    iva_porcentaje numeric(5,2) NOT NULL,
    iva_aplica boolean DEFAULT true NOT NULL,
    creado_en timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT detalle_reserva_cantidad_check CHECK ((cantidad > 0)),
    CONSTRAINT detalle_reserva_iva_check CHECK ((iva >= (0)::numeric)),
    CONSTRAINT detalle_reserva_iva_porcentaje_check CHECK ((iva_porcentaje >= (0)::numeric)),
    CONSTRAINT detalle_reserva_precio_dia_acordado_check CHECK ((precio_dia_acordado >= (0)::numeric)),
    CONSTRAINT detalle_reserva_subtotal_check CHECK ((subtotal >= (0)::numeric)),
    CONSTRAINT detalle_reserva_total_check CHECK ((total >= (0)::numeric))
);


--
-- Name: factura; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.factura (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    reserva_id uuid NOT NULL,
    numero_factura character varying(30) NOT NULL,
    tipo public.factura_tipo DEFAULT 'FACTURA'::public.factura_tipo NOT NULL,
    factura_original_id uuid,
    pago_reembolso_id uuid,
    subtotal numeric(14,2) NOT NULL,
    iva numeric(14,2) NOT NULL,
    total numeric(14,2) NOT NULL,
    moneda character(3) DEFAULT 'COP'::bpchar NOT NULL,
    iva_porcentaje numeric(5,2) NOT NULL,
    iva_aplica boolean DEFAULT true NOT NULL,
    datos_emisor jsonb NOT NULL,
    datos_receptor jsonb NOT NULL,
    fecha_emision timestamp with time zone DEFAULT now() NOT NULL,
    creado_en timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_factura_total CHECK ((total = (subtotal + iva))),
    CONSTRAINT chk_nota_credito_tiene_original CHECK (((tipo = 'FACTURA'::public.factura_tipo) OR ((tipo = 'NOTA_CREDITO'::public.factura_tipo) AND (factura_original_id IS NOT NULL)))),
    CONSTRAINT factura_iva_check CHECK ((iva >= (0)::numeric)),
    CONSTRAINT factura_iva_porcentaje_check CHECK ((iva_porcentaje >= (0)::numeric)),
    CONSTRAINT factura_subtotal_check CHECK ((subtotal >= (0)::numeric)),
    CONSTRAINT factura_total_check CHECK ((total >= (0)::numeric))
);


--
-- Name: factura_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.factura_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: herramienta; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.herramienta (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    categoria_id uuid NOT NULL,
    propietario_id uuid NOT NULL,
    nombre character varying(150) NOT NULL,
    descripcion text,
    marca character varying(50),
    modelo character varying(50),
    precio_dia numeric(14,2) NOT NULL,
    cantidad_total integer NOT NULL,
    estado_publicacion public.herramienta_pub_estado DEFAULT 'PUBLICADA'::public.herramienta_pub_estado NOT NULL,
    iva_porcentaje numeric(5,2) DEFAULT 19.00 NOT NULL,
    iva_aplica boolean DEFAULT true NOT NULL,
    creado_en timestamp with time zone DEFAULT now() NOT NULL,
    actualizado_en timestamp with time zone DEFAULT now() NOT NULL,
    cantidad_disponible integer DEFAULT 1,
    CONSTRAINT herramienta_cantidad_total_check CHECK ((cantidad_total > 0)),
    CONSTRAINT herramienta_iva_porcentaje_check CHECK ((iva_porcentaje >= (0)::numeric)),
    CONSTRAINT herramienta_precio_dia_check CHECK ((precio_dia >= (0)::numeric))
);


--
-- Name: pago; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pago (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    reserva_id uuid NOT NULL,
    pago_original_id uuid,
    estado public.pago_estado DEFAULT 'PENDIENTE'::public.pago_estado NOT NULL,
    proveedor_pago character varying(30) DEFAULT 'SIMULADO'::character varying NOT NULL,
    transaccion_id character varying(100),
    monto numeric(14,2) NOT NULL,
    moneda character(3) DEFAULT 'COP'::bpchar NOT NULL,
    motivo text,
    fecha_pago timestamp with time zone,
    creado_en timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT pago_monto_check CHECK ((monto > (0)::numeric))
);


--
-- Name: propietario; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.propietario (
    usuario_id uuid NOT NULL,
    razon_social character varying(200) NOT NULL,
    nit character varying(20) NOT NULL,
    direccion_fiscal text NOT NULL,
    telefono_contacto character varying(20),
    regimen character varying(30) NOT NULL,
    municipio character varying(100) NOT NULL,
    departamento character varying(100) NOT NULL,
    verificado_en timestamp with time zone
);


--
-- Name: reporte_dano; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.reporte_dano (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    detalle_reserva_id uuid NOT NULL,
    herramienta_id uuid NOT NULL,
    reportado_por_usuario_id uuid NOT NULL,
    descripcion text NOT NULL,
    severidad public.dano_severidad NOT NULL,
    costo_estimado_reparacion numeric(14,2),
    fecha_reporte timestamp with time zone DEFAULT now() NOT NULL,
    resuelto_en timestamp with time zone,
    resolucion text,
    CONSTRAINT reporte_dano_costo_estimado_reparacion_check CHECK ((costo_estimado_reparacion >= (0)::numeric))
);


--
-- Name: reserva; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.reserva (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    cliente_id uuid NOT NULL,
    estado public.reserva_estado DEFAULT 'PENDIENTE'::public.reserva_estado NOT NULL,
    fecha_inicio date NOT NULL,
    fecha_fin date NOT NULL,
    fecha_creacion timestamp with time zone DEFAULT now() NOT NULL,
    fecha_confirmacion timestamp with time zone,
    fecha_expiracion_pendiente timestamp with time zone,
    fecha_cancelacion timestamp with time zone,
    motivo_cancelacion text,
    cancelado_por_usuario_id uuid,
    actualizado_en timestamp with time zone DEFAULT now() NOT NULL,
    solicitud_devolucion boolean DEFAULT false,
    estado_devolucion character varying(50),
    nota_devolucion_cliente text,
    costo_reparacion numeric(14,2) DEFAULT 0,
    descripcion_dano text,
    observacion_propietario text,
    fecha_devolucion timestamp with time zone,
    CONSTRAINT chk_reserva_fechas CHECK ((fecha_fin >= fecha_inicio))
);


--
-- Name: usuario; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.usuario (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    email character varying(255) NOT NULL,
    password_hash character varying(255) NOT NULL,
    rol public.user_rol NOT NULL,
    nombre character varying(100) NOT NULL,
    apellido character varying(100) NOT NULL,
    telefono character varying(20),
    creado_en timestamp with time zone DEFAULT now() NOT NULL,
    actualizado_en timestamp with time zone DEFAULT now() NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: categoria categoria_nombre_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.categoria
    ADD CONSTRAINT categoria_nombre_key UNIQUE (nombre);


--
-- Name: categoria categoria_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.categoria
    ADD CONSTRAINT categoria_pkey PRIMARY KEY (id);


--
-- Name: cliente cliente_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cliente
    ADD CONSTRAINT cliente_pkey PRIMARY KEY (usuario_id);


--
-- Name: configuracion_fiscal configuracion_fiscal_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.configuracion_fiscal
    ADD CONSTRAINT configuracion_fiscal_pkey PRIMARY KEY (clave);


--
-- Name: detalle_reserva detalle_reserva_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.detalle_reserva
    ADD CONSTRAINT detalle_reserva_pkey PRIMARY KEY (id);


--
-- Name: factura factura_numero_factura_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.factura
    ADD CONSTRAINT factura_numero_factura_key UNIQUE (numero_factura);


--
-- Name: factura factura_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.factura
    ADD CONSTRAINT factura_pkey PRIMARY KEY (id);


--
-- Name: herramienta herramienta_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.herramienta
    ADD CONSTRAINT herramienta_pkey PRIMARY KEY (id);


--
-- Name: pago pago_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pago
    ADD CONSTRAINT pago_pkey PRIMARY KEY (id);


--
-- Name: propietario propietario_nit_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.propietario
    ADD CONSTRAINT propietario_nit_key UNIQUE (nit);


--
-- Name: propietario propietario_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.propietario
    ADD CONSTRAINT propietario_pkey PRIMARY KEY (usuario_id);


--
-- Name: reporte_dano reporte_dano_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reporte_dano
    ADD CONSTRAINT reporte_dano_pkey PRIMARY KEY (id);


--
-- Name: reserva reserva_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reserva
    ADD CONSTRAINT reserva_pkey PRIMARY KEY (id);


--
-- Name: usuario usuario_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuario
    ADD CONSTRAINT usuario_email_key UNIQUE (email);


--
-- Name: usuario usuario_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuario
    ADD CONSTRAINT usuario_pkey PRIMARY KEY (id);


--
-- Name: idx_detalle_reserva_herramienta; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_detalle_reserva_herramienta ON public.detalle_reserva USING btree (herramienta_id);


--
-- Name: idx_detalle_reserva_reserva; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_detalle_reserva_reserva ON public.detalle_reserva USING btree (reserva_id);


--
-- Name: idx_herramienta_categoria; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_herramienta_categoria ON public.herramienta USING btree (categoria_id);


--
-- Name: idx_herramienta_estado_pub; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_herramienta_estado_pub ON public.herramienta USING btree (estado_publicacion);


--
-- Name: idx_herramienta_propietario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_herramienta_propietario ON public.herramienta USING btree (propietario_id);


--
-- Name: idx_pago_original; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pago_original ON public.pago USING btree (pago_original_id) WHERE (pago_original_id IS NOT NULL);


--
-- Name: idx_pago_reserva; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pago_reserva ON public.pago USING btree (reserva_id);


--
-- Name: idx_reporte_dano_detalle; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_reporte_dano_detalle ON public.reporte_dano USING btree (detalle_reserva_id);


--
-- Name: idx_reporte_dano_fecha; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_reporte_dano_fecha ON public.reporte_dano USING btree (fecha_reporte);


--
-- Name: idx_reporte_dano_herramienta; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_reporte_dano_herramienta ON public.reporte_dano USING btree (herramienta_id);


--
-- Name: idx_reserva_cliente; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_reserva_cliente ON public.reserva USING btree (cliente_id);


--
-- Name: idx_reserva_estado; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_reserva_estado ON public.reserva USING btree (estado);


--
-- Name: idx_reserva_expiracion; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_reserva_expiracion ON public.reserva USING btree (fecha_expiracion_pendiente) WHERE (estado = 'PENDIENTE'::public.reserva_estado);


--
-- Name: idx_usuario_rol; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_usuario_rol ON public.usuario USING btree (rol);


--
-- Name: uq_factura_original_por_reserva; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_factura_original_por_reserva ON public.factura USING btree (reserva_id) WHERE (tipo = 'FACTURA'::public.factura_tipo);


--
-- Name: uq_factura_reserva_id; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_factura_reserva_id ON public.factura USING btree (reserva_id, id);


--
-- Name: uq_pago_completado_por_reserva; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_pago_completado_por_reserva ON public.pago USING btree (reserva_id) WHERE (estado = 'COMPLETADO'::public.pago_estado);


--
-- Name: detalle_reserva trg_detalle_reserva_disponibilidad; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_detalle_reserva_disponibilidad AFTER INSERT OR DELETE OR UPDATE ON public.detalle_reserva FOR EACH ROW EXECUTE FUNCTION public.trg_actualizar_disponibilidad_desde_detalle();


--
-- Name: detalle_reserva trg_detalle_reserva_inmutable; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_detalle_reserva_inmutable BEFORE UPDATE ON public.detalle_reserva FOR EACH ROW EXECUTE FUNCTION public.prevent_detalle_reserva_modification();


--
-- Name: herramienta trg_herramienta_actualizado; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_herramienta_actualizado BEFORE UPDATE ON public.herramienta FOR EACH ROW EXECUTE FUNCTION public.trigger_actualizado_en();


--
-- Name: pago trg_pago_completado_inmutable; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_pago_completado_inmutable BEFORE UPDATE ON public.pago FOR EACH ROW EXECUTE FUNCTION public.prevent_pago_completado_change();


--
-- Name: pago trg_proteger_reembolso_referenciado; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_proteger_reembolso_referenciado BEFORE UPDATE ON public.pago FOR EACH ROW EXECUTE FUNCTION public.proteger_reembolso_referenciado();


--
-- Name: reserva trg_reserva_actualizado; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_reserva_actualizado BEFORE UPDATE ON public.reserva FOR EACH ROW EXECUTE FUNCTION public.trigger_actualizado_en();


--
-- Name: reserva trg_reserva_estado_disponibilidad; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_reserva_estado_disponibilidad AFTER INSERT OR UPDATE OF estado ON public.reserva FOR EACH ROW EXECUTE FUNCTION public.trg_actualizar_disponibilidad_desde_reserva();


--
-- Name: reserva trg_reserva_fechas_inmutables; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_reserva_fechas_inmutables BEFORE UPDATE ON public.reserva FOR EACH ROW EXECUTE FUNCTION public.prevent_reserva_fechas_change();


--
-- Name: usuario trg_usuario_actualizado; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_usuario_actualizado BEFORE UPDATE ON public.usuario FOR EACH ROW EXECUTE FUNCTION public.trigger_actualizado_en();


--
-- Name: reserva trg_validar_expiracion_pendiente; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_validar_expiracion_pendiente BEFORE INSERT OR UPDATE ON public.reserva FOR EACH ROW EXECUTE FUNCTION public.validar_expiracion_pendiente();


--
-- Name: factura trg_validar_nota_credito_original; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_validar_nota_credito_original BEFORE INSERT OR UPDATE ON public.factura FOR EACH ROW EXECUTE FUNCTION public.validar_nota_credito_original();


--
-- Name: factura trg_validar_nota_credito_reembolso_factura; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_validar_nota_credito_reembolso_factura BEFORE INSERT OR UPDATE ON public.factura FOR EACH ROW EXECUTE FUNCTION public.validar_nota_credito_reembolso_factura();


--
-- Name: detalle_reserva trg_verificar_subtotal; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_verificar_subtotal BEFORE INSERT ON public.detalle_reserva FOR EACH ROW EXECUTE FUNCTION public.verificar_subtotal_detalle();


--
-- Name: cliente cliente_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cliente
    ADD CONSTRAINT cliente_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuario(id) ON DELETE CASCADE;


--
-- Name: detalle_reserva detalle_reserva_herramienta_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.detalle_reserva
    ADD CONSTRAINT detalle_reserva_herramienta_id_fkey FOREIGN KEY (herramienta_id) REFERENCES public.herramienta(id) ON DELETE RESTRICT;


--
-- Name: detalle_reserva detalle_reserva_reserva_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.detalle_reserva
    ADD CONSTRAINT detalle_reserva_reserva_id_fkey FOREIGN KEY (reserva_id) REFERENCES public.reserva(id) ON DELETE CASCADE;


--
-- Name: factura factura_reserva_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.factura
    ADD CONSTRAINT factura_reserva_id_fkey FOREIGN KEY (reserva_id) REFERENCES public.reserva(id) ON DELETE RESTRICT;


--
-- Name: factura fk_nota_credito_original; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.factura
    ADD CONSTRAINT fk_nota_credito_original FOREIGN KEY (reserva_id, factura_original_id) REFERENCES public.factura(reserva_id, id) DEFERRABLE;


--
-- Name: factura fk_nota_credito_reembolso; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.factura
    ADD CONSTRAINT fk_nota_credito_reembolso FOREIGN KEY (pago_reembolso_id) REFERENCES public.pago(id) DEFERRABLE;


--
-- Name: herramienta herramienta_categoria_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.herramienta
    ADD CONSTRAINT herramienta_categoria_id_fkey FOREIGN KEY (categoria_id) REFERENCES public.categoria(id) ON DELETE RESTRICT;


--
-- Name: herramienta herramienta_propietario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.herramienta
    ADD CONSTRAINT herramienta_propietario_id_fkey FOREIGN KEY (propietario_id) REFERENCES public.propietario(usuario_id) ON DELETE RESTRICT;


--
-- Name: pago pago_pago_original_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pago
    ADD CONSTRAINT pago_pago_original_id_fkey FOREIGN KEY (pago_original_id) REFERENCES public.pago(id);


--
-- Name: pago pago_reserva_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pago
    ADD CONSTRAINT pago_reserva_id_fkey FOREIGN KEY (reserva_id) REFERENCES public.reserva(id) ON DELETE RESTRICT;


--
-- Name: propietario propietario_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.propietario
    ADD CONSTRAINT propietario_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuario(id) ON DELETE CASCADE;


--
-- Name: reporte_dano reporte_dano_detalle_reserva_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reporte_dano
    ADD CONSTRAINT reporte_dano_detalle_reserva_id_fkey FOREIGN KEY (detalle_reserva_id) REFERENCES public.detalle_reserva(id) ON DELETE RESTRICT;


--
-- Name: reporte_dano reporte_dano_herramienta_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reporte_dano
    ADD CONSTRAINT reporte_dano_herramienta_id_fkey FOREIGN KEY (herramienta_id) REFERENCES public.herramienta(id) ON DELETE RESTRICT;


--
-- Name: reporte_dano reporte_dano_reportado_por_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reporte_dano
    ADD CONSTRAINT reporte_dano_reportado_por_usuario_id_fkey FOREIGN KEY (reportado_por_usuario_id) REFERENCES public.usuario(id);


--
-- Name: reserva reserva_cancelado_por_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reserva
    ADD CONSTRAINT reserva_cancelado_por_usuario_id_fkey FOREIGN KEY (cancelado_por_usuario_id) REFERENCES public.usuario(id);


--
-- Name: reserva reserva_cliente_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reserva
    ADD CONSTRAINT reserva_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES public.cliente(usuario_id) ON DELETE RESTRICT;


--
-- PostgreSQL database dump complete
--

\unrestrict 9IX04ZpS0nddEhgKd4iJCf3M8oygi3z4YZ77xmucSuZYXWkj6adk4eYP0cm9A7g

