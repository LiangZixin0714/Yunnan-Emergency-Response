package com.project.config;

import com.project.entity.mysql.Role;
import com.project.entity.mysql.User;
import com.project.repository.mysql.RoleRepository;
import com.project.repository.mysql.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        logger.info("开始初始化系统数据...");
        
        // 初始化角色
        initRoles();
        
        // 初始化用户
        initUsers();
        
        logger.info("系统数据初始化完成");
    }

    private void initRoles() {
        List<String> roleNames = Arrays.asList("ADMIN", "OPERATOR", "RESOURCE_MANAGER", "VIEWER");
        List<String> descriptions = Arrays.asList(
                "系统管理员，拥有所有权限",
                "指挥员，负责灾情处理和方案生成",
                "资源管理员，负责资源管理和调度",
                "信息员，仅查看权限"
        );

        for (int i = 0; i < roleNames.size(); i++) {
            String roleName = roleNames.get(i);
            String description = descriptions.get(i);
            
            if (roleRepository.findByRoleName(roleName).isEmpty()) {
                Role role = new Role();
                role.setRoleName(roleName);
                role.setDescription(description);
                roleRepository.save(role);
                logger.info("初始化角色: {}", roleName);
            } else {
                logger.info("角色已存在: {}", roleName);
            }
        }
    }

    private void initUsers() {
        List<UserData> users = Arrays.asList(
                new UserData("admin", "123456", "ADMIN", "系统管理员"),
                new UserData("operator", "123456", "OPERATOR", "李指挥"),
                new UserData("resource", "123456", "RESOURCE_MANAGER", "王资源"),
                new UserData("viewer", "123456", "VIEWER", "张信息员")
        );

        for (UserData userData : users) {
            if (!userRepository.existsByUsername(userData.username)) {
                Role role = roleRepository.findByRoleName(userData.roleName)
                        .orElseThrow(() -> new RuntimeException("角色不存在: " + userData.roleName));

                User user = new User();
                user.setUsername(userData.username);
                user.setPassword(passwordEncoder.encode(userData.password));
                user.setRealName(userData.realName);
                user.setRoleId(role.getId());
                user.setStatus(1);
                userRepository.save(user);
                logger.info("初始化用户: {} (角色: {})", userData.username, userData.roleName);
            } else {
                logger.info("用户已存在: {}", userData.username);
            }
        }
    }

    private static class UserData {
        String username;
        String password;
        String roleName;
        String realName;

        UserData(String username, String password, String roleName, String realName) {
            this.username = username;
            this.password = password;
            this.roleName = roleName;
            this.realName = realName;
        }
    }
}
