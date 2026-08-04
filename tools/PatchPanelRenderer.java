import java.nio.file.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

public final class PatchPanelRenderer {
  public static void main(String[] args)throws Exception{
    Path in=Path.of(args[0]),out=Path.of(args[1]); ClassNode c=new ClassNode();
    new ClassReader(Files.readAllBytes(in)).accept(c,0);
    MethodNode m=c.methods.stream().filter(x->x.name.equals("render")&&x.desc.equals("(Lnet/minecraft/class_310;Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;Ldev/thekimcreates/metrobuilder/client/psd/ClientPSDObject;I)V")).findFirst().orElseThrow();
    LabelNode normal=new LabelNode(); InsnList i=new InsnList();
    i.add(new VarInsnNode(Opcodes.ALOAD,1)); i.add(new VarInsnNode(Opcodes.ALOAD,2));
    i.add(new VarInsnNode(Opcodes.ALOAD,3)); i.add(new VarInsnNode(Opcodes.ILOAD,4));
    i.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"dev/thekimcreates/metrobuilder/client/psd/SingleGlassPanelRenderer","renderIfPanel","(Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;Ldev/thekimcreates/metrobuilder/client/psd/ClientPSDObject;I)Z",false));
    i.add(new JumpInsnNode(Opcodes.IFEQ,normal)); i.add(new InsnNode(Opcodes.RETURN)); i.add(normal);
    m.instructions.insertBefore(m.instructions.getFirst(),i);
    ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);c.accept(w);Files.write(out,w.toByteArray());
  }
}
