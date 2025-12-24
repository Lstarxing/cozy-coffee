package controller;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import service.UserService;
import model.User;
import service.LogService;
import java.io.File;
import javax.servlet.http.Part;
import javax.servlet.http.HttpSession;

@WebServlet("/users")
public class UserController extends HttpServlet {
    private UserService userService;
    private LogService logService;

    // 设置上传文件的存储路径
    private static final String UPLOAD_DIRECTORY = "web3/images/avatars";

    @Override
    public void init() throws ServletException {
        super.init();
        userService = new UserService();
        logService = new LogService();

        // 确保上传目录存在
        File uploadDir = new File(getServletContext().getRealPath(UPLOAD_DIRECTORY));
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
    }

    // 添加处理OPTIONS请求的方法,用于跨域预检
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setAccessControlHeaders(response);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    // 在所有请求处理方法开始时添加通用响应头设置
    private void setCommonResponseHeaders(HttpServletResponse response) {
        // 设置跨域头
        setAccessControlHeaders(response);
        // 设置响应类型和字符编码
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
    }

    private void setAccessControlHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*"); // 或指定域名
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.setHeader("Access-Control-Max-Age", "3600");
    }

    // 修改doPost方法示例
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setCommonResponseHeaders(response);
        
        try{
            String action = request.getParameter("action");
            if (action == null) {
                sendErrorResponse(response, "缺少action参数");
                return;
            }
            switch (action) {
                case "register":
                    registerUser(request, response);
                    break;
                case "login":
                    loginUser(request, response);
                    break;
                case "getUserInfo":
                    getUserInfo(request, response);
                    break;
                case "sendVerifyCode":
                    sendVerifyCode(request, response);
                    break;
                case "recoverPassword":
                    recoverPassword(request, response);
                    break;
                case "updateSelf":
                    updateUserSelf(request, response);
                    break;
                case "checkAvailability":
                    checkAvailability(request, response);
                    break;
                case "updateAvatar":
                    updateAvatar(request, response);
                    break;
                default:
                    sendErrorResponse(response, "未知的action类型: " + action);
            }
        } catch (Exception e) {
            e.printStackTrace();
            logService.log("ERROR", "请求处理异常", "错误: " + e.getMessage());
            sendErrorResponse(response, "服务器内部错误: " + e.getMessage());
        }
    }

    private void registerUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            // 记录请求参数
            String registerType = request.getParameter("registerType");//手机或邮箱注册
            String identifier = request.getParameter("identifier");//login_name:phone_number或email
            String verifyCode = request.getParameter("verifyCode");
            String password = request.getParameter("password");
            
            System.out.println("注册请求参数: registerType=" + registerType + 
                              ", identifier=" + identifier + 
                              ", verifyCode=" + verifyCode + 
                              ", password=******");
            
            // 验证参数
            if (identifier == null || password == null || verifyCode == null) {
                sendErrorResponse(response, "缺少必要参数");
                return;
            }

            // 验证验证码
            if (!userService.verifyCode(identifier, verifyCode)) {
                sendErrorResponse(response, "验证码错误或已过期");
                logService.log("WARN", "注册失败", "验证码错误, 用户: " + identifier);
                return;
            }

            // 创建用户对象
            User user = new User();
            user.setLoginName(identifier);
            user.setNickname(userService.generateRandomNickname());
            user.setInvitationCode(userService.generateInvitationCode());
            user.setMemberLevel("basic");
            user.setTotalPoints(0);
            user.setCurrentPoints(0);

            // 根据注册类型设置手机号或邮箱
            if ("phone".equals(registerType)) {
                if (userService.isPhoneNumberRegistered(identifier)) {
                    sendErrorResponse(response, "该手机号已注册");
                    return;
                }
                user.setPhoneNumber(identifier);
                user.setEmail("");
            } else if ("email".equals(registerType)) {
                if (userService.isEmailRegistered(identifier)) {
                    sendErrorResponse(response, "该邮箱已注册");
                    return;
                }
                user.setEmail(identifier);
                user.setPhoneNumber("");
            }

            // 注册用户
            boolean isRegistered = userService.registerUser(user, password);

            if (isRegistered) {
                logService.log("INFO", "注册成功", "用户: " + identifier);
                sendSuccessResponse(response, "注册成功");
            } else {
                logService.log("WARN", "注册失败", "用户: " + identifier);
                sendErrorResponse(response, "注册失败，该账号可能已被注册");
            }
        } catch (Exception e) {
            e.printStackTrace();
            logService.log("ERROR", "注册异常", "用户: " + request.getParameter("identifier") + ", 错误: " + e.getMessage());
            sendErrorResponse(response, "注册发生错误: " + e.getMessage());
        }
    }

    private void loginUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String loginName = request.getParameter("loginName");
            String password = request.getParameter("password");

            if (loginName == null || password == null) {
                sendErrorResponse(response, "缺少登录参数");
                return;
            }

            if (userService.validatePassword(loginName, password)) {
                User user = userService.getUserByLoginName(loginName);
                // 移除敏感信息
                user.setPasswordHash(null);
                
                // 在session中保存用户信息
                HttpSession session = request.getSession();
                session.setAttribute("userId", user.getId());
                session.setAttribute("userLevel", user.getMemberLevel());

                // 返回用户信息
                PrintWriter out = response.getWriter();
                out.print("{\"status\":\"success\", \"message\":\"登录成功\", \"data\":" + user.toJson() + "}");
                out.flush();
            } else {
                sendErrorResponse(response, "用户名或密码错误");
            }
        } catch (Exception e) {
            sendErrorResponse(response, "登录错误: " + e.getMessage());
        }
    }

    private void recoverPassword(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String type = request.getParameter("type");
            String identifier = request.getParameter("identifier");
            String verifyCode = request.getParameter("verifyCode");
            String newPassword = request.getParameter("newPassword");
  
            // 添加日志记录
            logService.log("INFO", "密码重置请求", 
                "类型: " + type + 
                ", 标识符: " + identifier + 
                ", 验证码: " + verifyCode);
  
            if (identifier == null || verifyCode == null || newPassword == null) {
                logService.log("WARN", "密码重置失败", "缺少参数");
                sendErrorResponse(response, "缺少必要参数");
                return;
            }
  
            // 验证验证码
            if (!userService.verifyCode(identifier, verifyCode)) {
                logService.log("WARN", "密码重置失败", "验证码错误, 用户: " + identifier);
                sendErrorResponse(response, "验证码错误或已过期");
                return;
            }
  
            // 根据类型调用不同的密码恢复方法
            boolean success = false;
            if ("phone".equals(type)) {
                success = userService.recoverPasswordByPhone(identifier, newPassword);
            } else if ("email".equals(type)) {
                success = userService.recoverPasswordByEmail(identifier, newPassword);
            } else {
                logService.log("WARN", "密码重置失败", "无效的账号类型: " + type);
                sendErrorResponse(response, "无效的账号类型");
                return;
            }
  
            if (success) {
                logService.log("INFO", "密码重置成功", "用户: " + identifier);
                sendSuccessResponse(response, "密码重置成功");
            } else {
                logService.log("WARN", "密码重置失败", "用户: " + identifier + " 未找到该账号");
                sendErrorResponse(response, "密码重置失败，该账号不存在");
            }
        } catch (Exception e) {
            e.printStackTrace(); // 添加堆栈跟踪
            logService.log("ERROR", "密码重置异常", "错误: " + e.getMessage());
            sendErrorResponse(response, "密码重置发生错误: " + e.getMessage());
        }
    }

    private void updateUserSelf(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            int userId = Integer.parseInt(request.getParameter("userId"));
            String nickname = request.getParameter("nickname");
            String email = request.getParameter("email");
            String phoneNumber = request.getParameter("phoneNumber");
            String newPassword = request.getParameter("newPassword");

            User user = new User();
            user.setId(userId);
            user.setNickname(nickname);
            user.setEmail(email);
            user.setPhoneNumber(phoneNumber);

            boolean updated = userService.updateUserBySelf(user, newPassword);
            if (updated) {
                sendSuccessResponse(response, "个人信息更新成功");
            } else {
                sendErrorResponse(response, "个人信息更新失败");
            }
        } catch (Exception e) {
            sendErrorResponse(response, "更新错误: " + e.getMessage());
        }
    }

    private void checkAvailability(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String type = request.getParameter("type");
            String value = request.getParameter("value");

            if (value == null) {
                sendErrorResponse(response, "缺少检查参数");
                return;
            }

            boolean isAvailable = false;
            switch (type) {
                case "loginName":
                    isAvailable = userService.isLoginNameAvailable(value);
                    break;
                case "phone":
                    isAvailable = !userService.isPhoneNumberRegistered(value);
                    break;
                case "email":
                    isAvailable = !userService.isEmailRegistered(value);
                    break;
                default:
                    sendErrorResponse(response, "无效的检查类型");
                    return;
            }

            if (isAvailable) {
                sendSuccessResponse(response, "可以使用");
            } else {
                sendErrorResponse(response, "已被占用");
            }
        } catch (Exception e) {
            sendErrorResponse(response, "检查错误: " + e.getMessage());
        }
    }

    private void sendVerifyCode(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            String type = request.getParameter("type"); // 标识符phone或email
            String target = request.getParameter("target"); // 手机号码或邮箱
            String operation = request.getParameter("operation"); // register或recoverPassword
  
            if (target == null || type == null) {
                sendErrorResponse(response, "缺少必要参数");
                return;
            }
  
            // 检查账号是否存在（仅对找回密码操作）
            boolean accountExists = false;
            if ("recoverPassword".equals(operation)) {
                if ("phone".equals(type)) {
                    accountExists = userService.isPhoneNumberRegistered(target);
                } else if ("email".equals(type)) {
                    accountExists = userService.isEmailRegistered(target);
                }
                
                if (!accountExists) {
                    sendErrorResponse(response, "该" + ("phone".equals(type) ? "手机号" : "邮箱") + "尚未注册，请先注册账号");
                    return;
                }
            } else if ("register".equals(operation)) {
                // 注册时检查账号是否已存在
                if ("phone".equals(type)) {
                    accountExists = userService.isPhoneNumberRegistered(target);
                } else if ("email".equals(type)) {
                    accountExists = userService.isEmailRegistered(target);
                }
                
                if (accountExists) {
                    sendErrorResponse(response, "该" + ("phone".equals(type) ? "手机号" : "邮箱") + "已注册，请直接登录");
                    return;
                }
            }
  
            boolean success = false;
            if ("phone".equals(type)) {
                success = userService.sendPhoneVerifyCode(target);
            } else if ("email".equals(type)) {
                success = userService.sendEmailVerifyCode(target);
            }
  
            if (success) {
                sendSuccessResponse(response, "验证码已发送");
            } else {
                sendErrorResponse(response, "验证码发送失败");
            }
        } catch (Exception e) {
            logService.log("ERROR", "发送验证码异常", "错误: " + e.getMessage());
            sendErrorResponse(response, "发送验证码失败: " + e.getMessage());
        }
    }

    // 获取用户信息
    private void getUserInfo(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 从session中获取用户ID
        Integer userId = (Integer) request.getSession().getAttribute("userId");
        if (userId == null) {
            sendErrorResponse(response, "未登录");
            return;
        }

        try {
            User user = userService.getUserById(userId);
            if (user != null) {
                // 构建用户信息JSON
                String userJson = String.format(
                    "{\"status\":\"success\"," +
                    "\"data\":{" +
                    "\"id\":%d," +
                    "\"nickname\":\"%s\"," +
                    "\"memberLevel\":\"%s\"," +
                    "\"totalPoints\":%d," +
                    "\"currentPoints\":%d," +
                    "\"email\":\"%s\"," +
                    "\"phoneNumber\":\"%s\"," +
                    "\"avatar\":\"%s\"" +
                    "}}", 
                    user.getId(),
                    user.getNickname(),
                    user.getMemberLevel(),
                    user.getTotalPoints(),
                    user.getCurrentPoints(),
                    user.getEmail(),
                    user.getPhoneNumber(),
                    user.getAvatar() != null ? user.getAvatar() : "images/default-avatar.png"
                );
                response.getWriter().write(userJson);
            } else {
                sendErrorResponse(response, "用户不存在");
            }
        } catch (Exception e) {
            sendErrorResponse(response, "获取用户信息失败: " + e.getMessage());
        }
    }

    // 添加统一的响应方法
    private void sendSuccessResponse(HttpServletResponse response, String message) throws IOException {
        PrintWriter out = response.getWriter();
        out.print("{\"status\":\"success\",\"message\":\"" + message + "\"}");
        out.flush();
    }
    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        PrintWriter out = response.getWriter();
        out.print("{\"status\":\"error\",\"message\":\"" + message + "\"}");
        out.flush();
    }

    private void updateAvatar(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        try {
            Part filePart = request.getPart("avatar");
            if (filePart == null) {
                sendErrorResponse(response, "未找到上传的文件");
                return;
            }

            // 验证文件类型
            String contentType = filePart.getContentType();
            if (!contentType.startsWith("image/")) {
                sendErrorResponse(response, "只允许上传图片文件");
                return;
            }

            // 生成文件名
            String fileName = System.currentTimeMillis() + "_" + getSubmittedFileName(filePart);
            String filePath = UPLOAD_DIRECTORY + File.separator + fileName;
            
            // 保存文件
            String realPath = getServletContext().getRealPath(filePath);
            filePart.write(realPath);
            
            // 更新用户头像路径
            HttpSession session = request.getSession();
            Integer userId = (Integer) session.getAttribute("userId");
            if (userId != null) {
                userService.updateUserAvatar(userId, filePath);
                // 返回新头像的URL
                String avatarUrl = request.getContextPath() + "/" + filePath;
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":\"success\",\"avatarUrl\":\"" + avatarUrl + "\"}");
            } else {
                sendErrorResponse(response, "用户未登录");
            }
        } catch (Exception e) {
            sendErrorResponse(response, "头像上传失败: " + e.getMessage());
        }
    }

    private String getSubmittedFileName(Part part) {
        String header = part.getHeader("content-disposition");
        if (header == null) return null;
        for (String token : header.split(";")) {
            if (token.trim().startsWith("filename")) {
                return token.substring(token.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
    }
}