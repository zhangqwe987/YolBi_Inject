package cn.yapeteam.loader.mixin;

import cn.yapeteam.loader.mixin.operation.Operation;
import cn.yapeteam.loader.mixin.operation.impl.InjectOperation;
import cn.yapeteam.loader.utils.ASMUtils;
import lombok.Getter;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Getter
public class Transformer {
    private final ClassProvider provider;
    private final ArrayList<Mixin> mixins;
    private final ArrayList<Operation> operations;

    public Transformer(ClassProvider classProvider) {
        this.provider = classProvider;
        this.mixins = new ArrayList<>();
        this.operations = new ArrayList<>();
        operations.add(new InjectOperation());
    }

    public void addMixin(String name) throws Throwable {
        addMixin(provider.getClassBytes(name));
    }

    public void addMixin(byte[] bytes) throws Throwable {
        addMixin(ASMUtils.node(bytes));
    }

    public void addMixin(ClassNode node) throws Throwable {
        mixins.add(new Mixin(node, provider));
    }

    public Map<String, byte[]> transform() {
        Map<String, byte[]> classMap = new HashMap<>();
        for (Mixin mixin : mixins) {
            for (Operation operation : operations)
                operation.dispose(mixin);
            String name = mixin.getTarget().name.replace('/', '.');
            byte[] class_bytes = ASMUtils.rewriteClass(mixin.getTarget());
            classMap.put(name, class_bytes);
        }
        return classMap;
    }
}
