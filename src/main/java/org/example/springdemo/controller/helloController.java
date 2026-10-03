package org.example.springdemo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController

public class helloController {

    /**
     * GET 测试，参数放在 URL 查询串（query string）里。
     * 例：http://localhost:8081/hello?nickname=Tom&phone=13800000000
     */
    @GetMapping("/hello")
    public String hello(String nickname, String phone) {
        System.out.println("GET  ->  nickname=" + nickname + ", phone=" + phone);
        return "Hello" + nickname;
    }

    /**
     * POST 表单测试，参数放在请求体里，Content-Type: application/x-www-form-urlencoded
     * 例：curl -X POST "http://localhost:8081/hello/form" -d "nickname=Tom&phone=13800000000"
     */
    @PostMapping("/hello/form")
    public String helloForm(@RequestParam String nickname, @RequestParam String phone) {
        System.out.println("POST(form)  ->  nickname=" + nickname + ", phone=" + phone);
        return "Hello " + nickname + ", phone=" + phone;
    }

    /**
     * POST JSON 测试，请求体是 JSON，Content-Type: application/json。
     * 例：curl -X POST "http://localhost:8081/hello/json"
     *            -H "Content-Type: application/json"
     *            -d '{"nickname":"Tom","phone":"13800000000"}'
     */
    @PostMapping("/hello/json")
    public String helloJson(@RequestBody User user) {
        System.out.println("POST(json)  ->  " + user);
        return "Hello " + user.nickname() + ", phone=" + user.phone();
    }

    /** 用 record 接收 JSON 请求体，写法更短（参数名必须和 JSON 字段一致）。 */
    public record User(String nickname, String phone) {
    }
}
