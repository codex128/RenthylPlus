/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.effects;

import codex.renthyl.modules.RenderContainer;
import codex.renthyl.modules.RenderModule;

/**
 * 
 * @author codex
 * @param <R>
 */
public class FilterStack <R extends RenderModule> extends RenderContainer<R> {
    
    public FilterStack() {
        super();
        addTickets();
    }
    public FilterStack(String name) {
        super(name);
        addTickets();
    }
    
    private void addTickets() {
        addInput("Color");
        addInput("Depth");
        addOutput("Result");
    }
    
    @Override
    public <T extends R> T add(T module, int index) {
        super.add(module, index);
        if (index > 0) {
            R prev = queue.get(index - 1);
            module.makeInput(prev, "Result", "Color");
        } else {
            makeInternalInput("Color", "Color", module);
        }
        if (index < queue.size()-1) {
            R next = queue.get(index + 1);
            next.makeInput(module, "Result", "Color");
        } else {
            makeInternalOutput(module, "Result", "Result");
        }
        if (module.getInput("Depth") != null) {
            makeInternalInput("Depth", "Depth", module);
        }
        return module;
    }
    
    @Override
    public boolean remove(R module) {
        int i = indexOf(module);
        if (i >= 0) {
            removeAndJoin(i);
            return true;
        }
        return false;
    }
    
    @Override
    public R remove(int i) {
        return removeAndJoin(i);
    }
    
    private R removeAndJoin(int i) {
        R prev = (i > 0 ? queue.get(i - 1) : null);
        R next = (i < queue.size() ? queue.get(i + 1) : null);
        R removed = super.remove(i);
        if (prev != null && next == null) {
            makeInternalOutput(prev, "Result", "Result");
        } else if (prev == null && next != null) {
            makeInternalInput("Color", "Color", next);
        } else if (prev != null && next != null) {
            next.makeInput(prev, "Result", "Color");
        }
        return removed;
    }
    
}
