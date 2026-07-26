package com.project.service;

import com.project.entity.mysql.Role;
import com.project.entity.mysql.User;
import com.project.repository.mysql.RoleRepository;
import com.project.repository.mysql.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MemberService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public MemberService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public List<MemberInfo> getMemberList(String keyword, String roleName) {
        List<User> users = userRepository.findAll();
        
        return users.stream()
                .filter(user -> {
                    if (keyword != null && !keyword.isEmpty()) {
                        String kw = keyword.toLowerCase();
                        boolean matchesKeyword = (user.getUsername() != null && user.getUsername().toLowerCase().contains(kw))
                                || (user.getRealName() != null && user.getRealName().toLowerCase().contains(kw));
                        if (!matchesKeyword) return false;
                    }
                    if (roleName != null && !roleName.isEmpty()) {
                        Role role = roleRepository.findById(user.getRoleId()).orElse(null);
                        if (role == null || !roleName.equals(role.getRoleName())) return false;
                    }
                    return true;
                })
                .map(this::convertToMemberInfo)
                .collect(Collectors.toList());
    }

    @Transactional("mysqlTransactionManager")
    public MemberInfo changeRole(Long userId, String targetRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在，id: " + userId));

        Role role = roleRepository.findByRoleName(targetRole)
                .orElseThrow(() -> new IllegalArgumentException("角色不存在: " + targetRole));

        user.setRoleId(role.getId());
        User saved = userRepository.save(user);
        return convertToMemberInfo(saved);
    }

    private MemberInfo convertToMemberInfo(User user) {
        Role role = roleRepository.findById(user.getRoleId()).orElse(null);
        String roleName = role != null ? role.getRoleName() : "VIEWER";

        return new MemberInfo(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getEmail(),
                user.getPhone(),
                roleName,
                user.getStatus(),
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : null,
                null
        );
    }

    public static class MemberInfo {
        private Long userId;
        private String username;
        private String realName;
        private String email;
        private String phone;
        private String roleName;
        private Integer status;
        private String registeredAt;
        private String lastLoginAt;

        public MemberInfo(Long userId, String username, String realName, String email, String phone,
                         String roleName, Integer status, String registeredAt, String lastLoginAt) {
            this.userId = userId;
            this.username = username;
            this.realName = realName;
            this.email = email;
            this.phone = phone;
            this.roleName = roleName;
            this.status = status;
            this.registeredAt = registeredAt;
            this.lastLoginAt = lastLoginAt;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getRealName() {
            return realName;
        }

        public void setRealName(String realName) {
            this.realName = realName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getRoleName() {
            return roleName;
        }

        public void setRoleName(String roleName) {
            this.roleName = roleName;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public String getRegisteredAt() {
            return registeredAt;
        }

        public void setRegisteredAt(String registeredAt) {
            this.registeredAt = registeredAt;
        }

        public String getLastLoginAt() {
            return lastLoginAt;
        }

        public void setLastLoginAt(String lastLoginAt) {
            this.lastLoginAt = lastLoginAt;
        }
    }
}
