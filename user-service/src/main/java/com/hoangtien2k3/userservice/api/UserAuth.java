package com.hoangtien2k3.userservice.api;

import com.hoangtien2k3.userservice.model.dto.request.SignInForm;
import com.hoangtien2k3.userservice.model.dto.request.SignUpForm;
import com.hoangtien2k3.userservice.model.dto.request.TokenValidationResponse;
import com.hoangtien2k3.userservice.model.dto.response.InformationMessage;
import com.hoangtien2k3.userservice.model.dto.response.JwtResponseMessage;
import com.hoangtien2k3.userservice.model.dto.response.ResponseMessage;
import com.hoangtien2k3.userservice.security.jwt.JwtProvider;
import com.hoangtien2k3.userservice.security.validate.AuthorityTokenUtil;
import com.hoangtien2k3.userservice.service.IUserService;
import com.hoangtien2k3.userservice.service.impl.UserServiceImpl;
import com.hoangtien2k3.userservice.security.validate.TokenValidate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class UserAuth {

    private final IUserService userService;
    private final JwtProvider jwtProvider;

    @Autowired
    public UserAuth(IUserService userService, JwtProvider jwtProvider) {
        this.userService = userService;
        this.jwtProvider = jwtProvider;
    }

    @PostMapping({"/signup", "/register"})
    public Mono<ResponseEntity<ResponseMessage>> register(@Validated @RequestBody SignUpForm signUpForm) {
        return userService.register(signUpForm)
                .flatMap(user -> Mono.just(new ResponseEntity<>(
                        new ResponseMessage("Create user: " + signUpForm.getUsername() + " successfully."),
                        HttpStatus.OK))
                )
                .onErrorResume(
                        error -> Mono.just(new ResponseEntity<>(new ResponseMessage(error.getMessage()), HttpStatus.BAD_REQUEST))
                );
    }

    @PostMapping({"/signin", "/login"})
    public Mono<ResponseEntity<JwtResponseMessage>> login(@Validated @RequestBody SignInForm signInForm) {
        return userService.login(signInForm)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    JwtResponseMessage errorjwtResponseMessage = new JwtResponseMessage(
                            null,
                            null,
                            new InformationMessage()
                    );
                    return Mono.just(new ResponseEntity<>(errorjwtResponseMessage, HttpStatus.UNAUTHORIZED));
                });
    }

//    @PostMapping({"/refresh", "/refresh-token"})
//    public Mono<ResponseEntity<JwtResponseMessage>> refresh(@RequestHeader("Refresh-Token") String refreshToken) {
//        return userService.refreshToken(refreshToken)
//                .map(newAccessToken -> {
//                    JwtResponseMessage jwtResponseMessage = new JwtResponseMessage(newAccessToken, null, null);
//                    return ResponseEntity.ok(jwtResponseMessage);
//                })
//                .onErrorResume(error -> Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
//    }

    @GetMapping({"/validateToken", "/validate-token"})
    public Boolean validateToken(@RequestHeader(name = "Authorization") String authorizationToken) {
        TokenValidate validate = new TokenValidate();
        if (validate.validateToken(authorizationToken)) {
            return ResponseEntity.ok(new TokenValidationResponse("Valid token")).hasBody();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new TokenValidationResponse("Invalid token")).hasBody();
        }
    }

    @GetMapping({"/hasAuthority", "/authorization"})
    public Boolean getAuthority(@RequestHeader(name = "Authorization") String authorizationToken,
                                String requiredRole) {
        AuthorityTokenUtil authorityTokenUtil = new AuthorityTokenUtil();
        List<String> authorities = authorityTokenUtil.checkPermission(authorizationToken);

        if(authorities.contains(requiredRole)) {
            return ResponseEntity.ok(new TokenValidationResponse("Role access api")).hasBody();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new TokenValidationResponse("Invalid token")).hasBody();
        }
    }

}