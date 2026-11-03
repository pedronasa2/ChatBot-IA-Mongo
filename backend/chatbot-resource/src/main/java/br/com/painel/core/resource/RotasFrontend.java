package br.com.painel.core.resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RotasFrontend {
    @GetMapping({"/", "/empresas/{empresaId}/candidatos", "/empresas/{empresaId}/agenda"})
    public String index() {
        return "forward:/index.html";
    }
}
