package com.project.service;

import com.project.dto.auth.LoginRequest;
import com.project.dto.auth.LoginResponse;
import com.project.dto.auth.RegisterRequest;
import com.project.entity.mysql.Role;
import com.project.entity.mysql.User;
import com.project.repository.mysql.RoleRepository;
import com.project.repository.mysql.UserRepository;
import com.project.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        if (user.getStatus() != 1) {
            throw new IllegalArgumentException("用户已被禁用");
        }

        String roleName = roleRepository.findById(user.getRoleId())
                .map(Role::getRoleName)
                .orElse("VIEWER");

        String token = jwtUtil.generateToken(user.getUsername(), roleName);

        return new LoginResponse(
                token,
                "Bearer",
                86400000L,
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                roleName
        );
    }

    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("用户名已存在");
        }

        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("邮箱已被注册");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRealName(request.getRealName());

        Role defaultRole = roleRepository.findByRoleName("VIEWER")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setRoleName("VIEWER");
                    role.setDescription("查看员");
                    return roleRepository.save(role);
                });
        user.setRoleId(defaultRole.getId());
        user.setStatus(1);

        return userRepository.save(user);
    }
}