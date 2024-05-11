package joao_rodrigo.demo.googol;

import java.lang.ProcessBuilder.Redirect;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.RequestParam;
@Controller
public class GoogolController {
  @GetMapping("/googol/new")
  public String googol(Model model) {
    model.addAttribute("googol", new Googol());
    return "googol/new";
  }
  @PostMapping("/googol/new")
  public String googolPost(Googol googol, RedirectAttributes redirectAttributes) {
    System.out.println(googol.getUrl());
    redirectAttributes.addFlashAttribute("message",String.format("%s was added successfully!", googol.getUrl()));
    return "redirect:/";
  }

  @GetMapping("/googol/search")
  public String search(@RequestParam(required = false) String term, @RequestParam(required = false) String page){
    System.out.println(term);
    System.out.println(page);
    return "googol/search";
  }
}
