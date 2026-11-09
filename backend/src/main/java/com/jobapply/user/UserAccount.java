package com.jobapply.user;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="users")
public class UserAccount {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="full_name",nullable=false,length=100) private String fullName;
    @Column(nullable=false,unique=true,length=150) private String email;
    @Column(name="password_hash",nullable=false) private String passwordHash;
    @Column(nullable=false,length=30) private String role="USER";
    @Column(nullable=false) private boolean enabled=true;
    @Column(name="created_at",nullable=false) private Instant createdAt=Instant.now();
    public Long getId(){return id;} public String getFullName(){return fullName;} public void setFullName(String v){fullName=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getPasswordHash(){return passwordHash;} public void setPasswordHash(String v){passwordHash=v;}
}
