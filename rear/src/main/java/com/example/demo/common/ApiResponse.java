package com.example.demo.common;

import com.example.demo.constant.ErrorCode;
import lombok.Data;
// 响应体
// 用于封装API调用的结果，包括状态码、消息、数据和时间戳
// 通用的响应体，用于返回API调用的结果
//@Data 正常位于开发中的common文件夹下 对吗？ 是的 和其他response类放一起不利于维护
//如果我新建一个common文件夹，放这个ApiResponse类 那些文件的调用需要修改
//分文件夹吧 controller 下的文件需要修改
//service 下的文件需要修改
//repository 下的文件需要修改
@Data
public class ApiResponse<T> {
    private Integer code;
    private String message;
    private T data;
    private Long timestamp;
    // 构造函数
    // 初始化响应体，设置默认状态码为200，消息为"success"
    // 【技术说明】用于创建成功响应
    public ApiResponse() {
        this.timestamp = System.currentTimeMillis();// 初始化时间戳为当前时间毫秒数
    }
    // 创建成功响应
    // 【技术说明】用于创建成功响应，包含数据
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("success");
        response.setData(data);
        return response;
    }
    // 创建成功响应
    // 【技术说明】用于创建成功响应，包含自定义消息和数据
    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage(message);
        response.setData(data);
        return response;
    }
    // 创建错误响应
    // 【技术说明】用于创建错误响应，包含自定义状态码和消息
    public static <T> ApiResponse<T> error(Integer code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        return response;
    }
    // 创建错误响应
    // 【技术说明】用于创建错误响应，包含默认状态码500和自定义消息
    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(500);
        response.setMessage(message);
        return response;
    }

    /**
     * 创建错误响应（使用ErrorCode枚举）
     * 
     * @param errorCode 错误码枚举
     * @param message 错误信息
     * @param <T> 数据类型
     * @return 错误响应
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(errorCode.getCode());
        response.setMessage(message);
        return response;
    }

    /**
     * 创建错误响应（使用ErrorCode枚举，包含数据）
     * 
     * @param errorCode 错误码枚举
     * @param message 错误信息
     * @param data 错误数据
     * @param <T> 数据类型
     * @return 错误响应
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(errorCode.getCode());
        response.setMessage(message);
        response.setData(data);
        return response;
    }
}