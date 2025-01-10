/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.tests;

/**
 *
 * @author codex
 */
public class TestMisc {
    
    public static void main(String[] args) {
        int key = ("LWJGL".hashCode() ^ "3".hashCode()) & 0xFFF;
        System.out.println(key);
    }
    
}
