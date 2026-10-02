package {{package}}.app;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public final class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("name", "{{name}}");
        return "home";
    }
}
