/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.effects;

import codex.renthyl.modules.RenderContainer;
import codex.renthyl.modules.protocol.FilterProtocol;
import codex.renthyl.resources.tickets.TicketSelector;
import codex.renthyl.resources.tickets.TicketSignature;

/**
 * 
 * @author codex
 * @param <R>
 */
public class FilterChain <R extends FilterProtocol> extends RenderContainer<R> implements FilterProtocol {
    
    private static final TicketSignature<TicketSelector.NameSelector>
            color = new TicketSignature<>(true, TicketSelector.name("Color")),
            depth = new TicketSignature<>(true, TicketSelector.name("Depth")),
            result = new TicketSignature<>(false, TicketSelector.name("Result"));
    
    public FilterChain() {
        super();
        addTickets();
    }
    public FilterChain(String name) {
        super(name);
        addTickets();
    }
    
    private void addTickets() {
        addInput(color.getSelector().getName());
        addInput(depth.getSelector().getName());
        addOutput(result.getSelector().getName());
        makeInput(this, color, result);
    }
    
    @Override
    public <T extends R> T add(T module, int index) {
        super.add(module, index);
        if (index > 0) {
            R prev = queue.get(index - 1);
            module.makeInput(prev, prev.getFilteredResult(), module.getRenderedSceneColor());
        } else {
            module.makeInput(this, color, module.getRenderedSceneColor());
        }
        if (index < queue.size()-1) {
            R next = queue.get(index + 1);
            next.makeInput(module, module.getFilteredResult(), next.getRenderedSceneColor());
        } else {
            makeInput(module, module.getFilteredResult(), result);
        }
        module.makeInput(this, depth, module.getRenderedSceneDepth());
        return module;
    }
    @Override
    public R remove(int i) {
        R prev = (i > 0 ? queue.get(i - 1) : null);
        R next = (i < queue.size() ? queue.get(i + 1) : null);
        R removed = super.remove(i);
        if (prev != null && next == null) {
            makeInput(prev, prev.getFilteredResult(), result);
        } else if (prev == null && next != null) {
            next.makeInput(this, color, next.getRenderedSceneColor());
        } else if (prev != null && next != null) {
            next.makeInput(prev, prev.getFilteredResult(), next.getRenderedSceneColor());
        } else {
            makeInput(this, color, result);
        }
        return removed;
    }
    @Override
    public TicketSignature getRenderedSceneColor() {
        return color;
    }
    @Override
    public TicketSignature getRenderedSceneDepth() {
        return depth;
    }
    @Override
    public TicketSignature getFilteredResult() {
        return result;
    }
    
}
