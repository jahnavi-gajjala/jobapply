package com.jobapply.user;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api/v1/users")
public class UserController {
 private final UserRepository users;
 public UserController(UserRepository users){this.users=users;}
 @GetMapping("/me") public Map<String,Object> me(Authentication auth){
  var u=users.findByEmail(auth.getName()).orElseThrow();
  return Map.of("id",u.getId(),"fullName",u.getFullName(),"email",u.getEmail());
 }
}
