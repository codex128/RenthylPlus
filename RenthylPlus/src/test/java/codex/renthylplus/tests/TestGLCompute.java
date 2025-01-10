/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.tests;

import org.lwjgl.opengl.GL43;
import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 *
 * @author codex
 */
public class TestGLCompute {
    
    private static String codeSource = "to-be-implemented";
    
    public static void main(String[] args) {
        
        int compute = glCreateShader(GL_COMPUTE_SHADER);
        glShaderSource(compute, codeSource);
        glCompileShader(compute);
        int program = glCreateProgram();
        glAttachShader(program, compute);
        glLinkProgram(program);
        
        int w = 512;
        int h = 512;
        int texture = glGenTextures();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, w, h, 0, GL_RGBA, GL_FLOAT, NULL);
        glBindImageTexture(0, texture, 0, false, 0, GL_READ_ONLY, GL_RGBA32F);
        
        glUseProgram(program);
        glUniform1i(glGetUniformLocation(program, "MyUniform"), 15);
        glDispatchCompute(w, h, 1);
        glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        
    }
    
}
