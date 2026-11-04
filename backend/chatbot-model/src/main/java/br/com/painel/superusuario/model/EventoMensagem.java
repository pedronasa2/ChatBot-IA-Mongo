package br.com.painel.superusuario.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "eventos_mensagens")
public record EventoMensagem(@Id ObjectId id, UUID empresaId, Instant enviadoEm) {}
