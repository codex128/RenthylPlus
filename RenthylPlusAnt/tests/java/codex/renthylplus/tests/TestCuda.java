/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package codex.renthylplus.tests;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.cuda.CU;
import org.lwjgl.cuda.CUGL;
import org.lwjgl.cuda.NVRTC;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.system.Callback;
import org.lwjgl.system.MemoryStack;

/**
 *
 * @author codex
 */
public class TestCuda {
    
    public static void main(String[] args) {
        try (MemoryStack frame = MemoryStack.stackPush()) {
            run(frame);
        }
    }
    
    private static void run(MemoryStack stack) {
        
        glfwInit();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        long window = glfwCreateWindow(512, 512, "Hello CUDA!", NULL, NULL);
        GLFWKeyCallback keyCallback;
        glfwSetKeyCallback(window, keyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (action == GLFW_PRESS && key == GLFW_KEY_ESCAPE)
                    glfwSetWindowShouldClose(window, true);
            }
        });
        glfwMakeContextCurrent(window);
        createCapabilities();
        Callback debugProc = GLUtil.setupDebugMessageCallback();
        int tex = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, tex);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 512, 512, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
        glEnable(GL_TEXTURE_2D);
        
        IntBuffer count = stack.mallocInt(1);
        IntBuffer dev = stack.mallocInt(1);
        PointerBuffer ctx = stack.mallocPointer(1);
        PointerBuffer resource = stack.mallocPointer(1);
        PointerBuffer array = stack.mallocPointer(1);
        PointerBuffer module = stack.mallocPointer(1);
        PointerBuffer surfref = stack.mallocPointer(1);
        PointerBuffer function = stack.mallocPointer(1);
        
        check(CU.cuInit(0));
        check(CU.cuDeviceGetCount(count));
        assert count.get(0) != 0 : "No CUDA devices found.";
        check(CU.cuDeviceGet(dev, 0));
        check(CU.cuCtxCreate(ctx, 0, dev.get(0)));
        
        PointerBuffer program = stack.mallocPointer(1);
        PointerBuffer headers = stack.mallocPointer(1);
        PointerBuffer includeNames = stack.mallocPointer(1);
        PointerBuffer options = stack.mallocPointer(1);
        PointerBuffer codeSize = stack.mallocPointer(1);
        
        String src = "";
        
        check(NVRTC.nvrtcCreateProgram(program, src, "MyCuda.cu", headers, includeNames));
        long prog = program.get(0);
        check(NVRTC.nvrtcCompileProgram(program.get(0), options));
        check(NVRTC.nvrtcGetCUBINSize(prog, codeSize));
        ByteBuffer code = stack.malloc(codeSize.getIntBuffer(0, 1).get(0));
        NVRTC.nvrtcGetCUBIN(prog, code);
        
        String ptx =
// Minimum PTX version 1.5 to be able to use .surfref and sust
".version 1.5\n" + 
// We make no use of actual shader model capabilities/functions, so target the lowest possible
".target sm_11\n" +
// Add a global reference to a surface which we will write to
".global .surfref surface;\n" +
// Function to write color to a surface
".visible .entry fillcolor () {\n" +
// Allocate some registers to compute the thread (x, y) coordinates
"   .reg .u32       %blockid, %blockdim, %thrid, %xidx, %yidx, %w;\n" +
// Allocate float registers for floating-point calculations
"   .reg .f32       %fwidth, %xpos;\n" +
// Allocate a u8 register to hold the red color channel value to write
"   .reg .u8        %red;\n" +
// Compute the x coordinate of this thread for writing to the surface
// xidx = ctaid.x * ntid.x + tid.x
"    mov.u32        %blockid, %ctaid.x;\n" +
"    mov.u32        %blockdim, %ntid.x;\n" +
"    mov.u32        %thrid, %tid.x;\n" +
"    mad.lo.u32     %xidx, %blockid, %blockdim, %thrid;\n" +
// Compute the y coordinate of this thread for writing to the surface
// yidx = ctaid.y * ntid.y + tid.y
"    mov.u32        %blockid, %ctaid.y;\n" +
"    mov.u32        %blockdim, %ntid.y;\n" +
"    mov.u32        %thrid, %tid.y;\n" +
"    mad.lo.u32     %yidx, %blockid, %blockdim, %thrid;\n" +
// Compute color based on interpolated x coordinate (from 0 to 255)
// Convert the x coordinate to a float
"    cvt.rn.f32.u32 %xpos, %xidx;\n" +
// Obtain the width of the surface
"    suq.width.b32  %w, [surface];\n" +
// Convert the width to a float
"    cvt.rn.f32.u32 %fwidth, %w;\n" +
// Compute the reciprocal (1.0f/fwidth)
"    rcp.approx.f32 %fwidth, %fwidth;\n" +
// Multiply 1/fwidth to the x coordinate
"    mul.f32        %xpos, %xpos, %fwidth;\n" +
// Multiply by 255.0f to get to the range [0, 255)
"    mul.f32        %xpos, %xpos, 0F437f0000;\n" + // 255.0f
// Convert to u8 for storing via sust
"    cvt.rni.u8.f32 %red, %xpos;\n" +
// Write color to surface
// Pay close attention to the documentation of the sust instruction!
// "The lowest dimension coordinate represents a byte offset into the surface and is not scaled."
// So we have to multiply xidx by 4 in order to get the actual texel byte offset:
"    shl.b32        %xidx, %xidx, 2U;\n" +
"    sust.b.2d.v4.b8.trap [surface, {%xidx, %yidx}], {%red, 0, 0, 255};\n" +
"}";
        
        check(CUGL.cuGraphicsGLRegisterImage(resource, tex, GL_TEXTURE_2D,
                CU.CU_GRAPHICS_REGISTER_FLAGS_WRITE_DISCARD |
                CU.CU_GRAPHICS_REGISTER_FLAGS_SURFACE_LDST));
        check(CU.cuGraphicsMapResources(resource, NULL));
        check(CU.cuGraphicsSubResourceGetMappedArray(array, resource.get(0), 0, 0));
        check(CU.cuGraphicsUnmapResources(resource, NULL));
        check(CU.cuModuleLoadData(module, stack.ASCII(ptx)));
        //check(CU.cuModuleLoadData(module, code));
        check(CU.cuModuleGetSurfRef(surfref, module.get(0), "surface"));
        check(CU.cuSurfRefSetArray(surfref.get(0), array.get(0), 0));
        check(CU.cuModuleGetFunction(function, module.get(0), "fillcolor"));
        check(CU.cuLaunchKernel(function.get(0), 64, 64, 1, 8, 8, 1, 0, 0, null, null));
        check(CU.cuCtxSynchronize());
        check(CU.cuCtxDestroy(ctx.get(0)));
        
        glfwShowWindow(window);
        while (!glfwWindowShouldClose(window)) {
            glBegin(GL_QUADS);
            glTexCoord2f(0, 0); glVertex2f(-1, -1);
            glTexCoord2f(1, 0); glVertex2f(+1, -1);
            glTexCoord2f(1, 1); glVertex2f(+1, +1);
            glTexCoord2f(0, 1); glVertex2f(-1, +1);
            glEnd();
            glfwSwapBuffers(window);
            glfwPollEvents();
        }
        glfwDestroyWindow(window);
        glfwTerminate();
        if (debugProc != null)
            debugProc.free();
        keyCallback.free();
        GL.setCapabilities(null);
        
    } 
    
    private static void check(int err) {
        if (err != 0)
            throw new AssertionError("Error code: " + err);
    }
    
}
