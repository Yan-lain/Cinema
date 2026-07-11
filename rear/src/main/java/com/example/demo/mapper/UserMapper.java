package com.example.demo.mapper;

import com.example.demo.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM user")
    List<User> findAll();

    @Select("SELECT * FROM user WHERE id = #{id}")
    User findById(Long id);

    @Select("SELECT * FROM user WHERE username = #{username}")
    User findByUsername(String username);

    @Select("SELECT COUNT(*) FROM user WHERE username = #{username}")
    int countByUsername(String username);
    
    @Select("SELECT COUNT(*) FROM user WHERE email = #{email}")
    int countByEmail(String email);
    
    @Select("SELECT * FROM user WHERE email = #{email}")
    User findByEmail(String email);

    @Select("SELECT * FROM user WHERE role = #{role}")
    List<User> findByRole(String role);

    @Select("SELECT * FROM user WHERE status = #{status}")
    List<User> findByStatus(String status);

    @Insert("INSERT INTO user(username, password, nickname, phone, email, role, status) " +
            "VALUES(#{username}, #{password}, #{nickname}, #{phone}, #{email}, #{role}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE user SET username=#{username}, nickname=#{nickname}, phone=#{phone}, " +
            "email=#{email}, role=#{role}, status=#{status} WHERE id=#{id}")
    int update(User user);

    @Update("UPDATE user SET password=#{password} WHERE id=#{id}")
    int updatePassword(Long id, String password);

    @Update("UPDATE user SET status=#{status} WHERE id=#{id}")
    int updateStatus(Long id, String status);

    @Delete("DELETE FROM user WHERE id=#{id}")
    int deleteById(Long id);
}
