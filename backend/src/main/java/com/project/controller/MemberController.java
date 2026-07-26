package com.project.controller;

import com.project.common.Result;
import com.project.service.MemberService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/member")
@Validated
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Result<List<MemberService.MemberInfo>>> getMemberList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String roleName) {
        List<MemberService.MemberInfo> members = memberService.getMemberList(keyword, roleName);
        return ResponseEntity.ok(Result.success(members));
    }

    @PostMapping("/change-role")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Result<Map<String, Object>>> changeMemberRole(
            @Valid @RequestBody ChangeRoleRequest request) {
        MemberService.MemberInfo member = memberService.changeRole(request.getUserId(), request.getTargetRole());
        return ResponseEntity.ok(Result.success(Map.of(
                "userId", member.getUserId(),
                "roleName", member.getRoleName()
        )));
    }

    public static class ChangeRoleRequest {
        @NotNull(message = "userId不能为空")
        private Long userId;

        @NotBlank(message = "targetRole不能为空")
        private String targetRole;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getTargetRole() {
            return targetRole;
        }

        public void setTargetRole(String targetRole) {
            this.targetRole = targetRole;
        }
    }
}
