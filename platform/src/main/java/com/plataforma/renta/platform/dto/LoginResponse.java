package com.plataforma.renta.platform.dto;

public record LoginResponse(String token, String tipo, String email, String rol) {}