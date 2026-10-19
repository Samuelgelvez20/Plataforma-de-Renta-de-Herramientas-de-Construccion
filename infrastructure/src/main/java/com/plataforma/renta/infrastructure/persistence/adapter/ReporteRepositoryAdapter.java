package com.plataforma.renta.infrastructure.persistence.adapter;

import com.plataforma.renta.domain.port.out.ReporteRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class ReporteRepositoryAdapter implements ReporteRepository {

    private final JdbcTemplate jdbcTemplate;

    public ReporteRepositoryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<IngresoMensualData> findIngresosMensuales() {
        String sql = "SELECT TO_CHAR(COALESCE(p.fecha_pago, p.creado_en), 'YYYY-MM') AS mes, " +
                     "COALESCE(SUM(p.monto), 0) AS total " +
                     "FROM pago p " +
                     "WHERE p.estado = 'COMPLETADO' " +
                     "  AND p.pago_original_id IS NULL " +
                     "GROUP BY TO_CHAR(COALESCE(p.fecha_pago, p.creado_en), 'YYYY-MM') " +
                     "ORDER BY mes ASC";

        return jdbcTemplate.query(sql, (rs, rowNum) -> new IngresoMensualData(
                rs.getString("mes"),
                rs.getBigDecimal("total") != null ? rs.getBigDecimal("total") : BigDecimal.ZERO
        ));
    }

    @Override
    public List<HerramientaAlquiladaData> findHerramientasMasAlquiladas(int limite) {
        int max = limite > 0 ? limite : 5;
        String sql = "SELECT CAST(h.id AS VARCHAR) AS hid, " +
                     "h.nombre AS hnombre, " +
                     "COUNT(dr.id) AS veces, " +
                     "h.precio_dia AS precio " +
                     "FROM detalle_reserva dr " +
                     "JOIN herramienta h ON dr.herramienta_id = h.id " +
                     "JOIN reserva r ON dr.reserva_id = r.id " +
                     "WHERE r.estado NOT IN ('CANCELADA', 'EXPIRADA') " +
                     "GROUP BY h.id, h.nombre, h.precio_dia " +
                     "ORDER BY veces DESC " +
                     "LIMIT ?";

        return jdbcTemplate.query(sql, (rs, rowNum) -> new HerramientaAlquiladaData(
                rs.getString("hid"),
                rs.getString("hnombre"),
                rs.getLong("veces"),
                rs.getBigDecimal("precio") != null ? rs.getBigDecimal("precio") : BigDecimal.ZERO
        ), max);
    }

    @Override
    public List<ClienteFrecuenteData> findClientesFrecuentes(int limite) {
        int max = limite > 0 ? limite : 5;
        String sql = "SELECT CONCAT(u.nombre, ' ', u.apellido) AS cnombre, " +
                     "COUNT(DISTINCT r.id) AS tot_reservas, " +
                     "COALESCE(SUM(p.monto), 0) AS tot_gasto " +
                     "FROM usuario u " +
                     "JOIN reserva r ON r.cliente_id = u.id AND r.estado NOT IN ('CANCELADA', 'EXPIRADA') " +
                     "LEFT JOIN (" +
                     "    SELECT reserva_id, SUM(monto) AS monto " +
                     "    FROM pago " +
                     "    WHERE estado = 'COMPLETADO' AND pago_original_id IS NULL " +
                     "    GROUP BY reserva_id" +
                     ") p ON p.reserva_id = r.id " +
                     "GROUP BY u.id, u.nombre, u.apellido " +
                     "ORDER BY tot_reservas DESC, tot_gasto DESC " +
                     "LIMIT ?";

        return jdbcTemplate.query(sql, (rs, rowNum) -> new ClienteFrecuenteData(
                rs.getString("cnombre"),
                rs.getLong("tot_reservas"),
                rs.getBigDecimal("tot_gasto") != null ? rs.getBigDecimal("tot_gasto") : BigDecimal.ZERO
        ), max);
    }

    @Override
    public MetricasResumenData obtenerMetricasResumen() {
        long totalUsuarios = queryForLong("SELECT COUNT(*) FROM usuario");
        long totalClientes = queryForLong("SELECT COUNT(*) FROM cliente");
        long totalPropietarios = queryForLong("SELECT COUNT(*) FROM propietario");
        long totalHerramientas = queryForLong("SELECT COUNT(*) FROM herramienta");
        long herramientasDisponibles = queryForLong("SELECT COUNT(*) FROM herramienta WHERE cantidad_disponible > 0");
        long totalReservas = queryForLong("SELECT COUNT(*) FROM reserva");
        long reservasActivas = queryForLong("SELECT COUNT(*) FROM reserva WHERE estado IN ('CONFIRMADA', 'EN_CURSO')");
        BigDecimal totalIngresos = queryForBigDecimal("SELECT COALESCE(SUM(monto), 0) FROM pago WHERE estado = 'COMPLETADO' AND pago_original_id IS NULL");
        long totalFacturas = queryForLong("SELECT COUNT(*) FROM factura WHERE tipo = 'FACTURA'");
        long reembolsosPendientes = queryForLong("SELECT COUNT(*) FROM pago WHERE pago_original_id IS NOT NULL AND estado = 'PENDIENTE'");

        return new MetricasResumenData(
                totalUsuarios,
                totalClientes,
                totalPropietarios,
                totalHerramientas,
                herramientasDisponibles,
                totalReservas,
                reservasActivas,
                totalIngresos,
                totalFacturas,
                reembolsosPendientes
        );
    }

    private long queryForLong(String sql) {
        Long val = jdbcTemplate.queryForObject(sql, Long.class);
        return val != null ? val : 0L;
    }

    private BigDecimal queryForBigDecimal(String sql) {
        BigDecimal val = jdbcTemplate.queryForObject(sql, BigDecimal.class);
        return val != null ? val : BigDecimal.ZERO;
    }
}
