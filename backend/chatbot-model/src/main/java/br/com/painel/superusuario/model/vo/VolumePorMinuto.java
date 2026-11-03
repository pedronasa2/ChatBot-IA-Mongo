package br.com.painel.superusuario.model.vo;

import java.time.Instant;
import java.util.*;

public record VolumePorMinuto(Instant minuto, long mensagens) {}
