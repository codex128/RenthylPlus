/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.effects;

import codex.renthyl.modules.RenderContainer;
import codex.renthyl.modules.RenderModule;
import codex.renthyl.resources.tickets.ResourceTicket;
import codex.renthyl.resources.tickets.TicketSelector;
import com.jme3.texture.Texture2D;

/**
 * 
 * @author codex
 * @param <R>
 */
public class FilterChain <R extends RenderModule> extends RenderContainer<R> {
    
    private static final TicketSelector color = TicketSelector.name("Color");
    private static final TicketSelector depth = TicketSelector.name("Depth");
    private static final TicketSelector result = TicketSelector.name("Result");
    
    public FilterChain() {
        super();
        addTickets();
    }
    public FilterChain(String name) {
        super(name);
        addTickets();
    }
    
    private void addTickets() {
        addInput("Color");
        addInput("Depth");
        addOutput("Result");
        getMainOutputGroup().makeInput(getMainInputGroup(), color, result);
    }
    
    @Override
    public <T extends R> T add(T module, int index) {
        super.add(module, index);
        if (index > 0) {
            R prev = queue.get(index - 1);
            module.getMainInputGroup().makeInput(prev.getMainOutputGroup(), result, color);
        } else {
            makeInternalInput(color, color, module);
        }
        if (index < queue.size()-1) {
            R next = queue.get(index + 1);
            next.getMainInputGroup().makeInput(module.getMainOutputGroup(), result, color);
        } else {
            makeInternalOutput(module, result, result);
        }
        if (module.getMainInputGroup().select(depth) != null) {
            makeInternalInput(depth, depth, module);
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
            makeInternalOutput(prev, result, result);
        } else if (prev == null && next != null) {
            makeInternalInput(color, color, next);
        } else if (prev != null && next != null) {
            next.getMainInputGroup().makeInput(prev.getMainOutputGroup(), result, color);
        } else {
            getMainOutputGroup().makeInput(getMainInputGroup(), color, result);
        }
        return removed;
    }
    
}
