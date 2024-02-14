package cn.yapeteam.yolbi.module.impl.visual;

import cn.yapeteam.yolbi.YolBi;
import cn.yapeteam.yolbi.event.Listener;
import cn.yapeteam.yolbi.event.impl.render.EventRender2D;
import cn.yapeteam.yolbi.font.AbstractFontRenderer;
import cn.yapeteam.yolbi.module.Module;
import cn.yapeteam.yolbi.module.ModuleCategory;
import cn.yapeteam.yolbi.module.ModuleInfo;

import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@ModuleInfo(name = "HUD", category = ModuleCategory.VISUAL)
public class HeadUpDisplay extends Module {
    private ClientTheme theme = null;

    @Listener
    private void onRender(EventRender2D e) {
        if (theme == null)
            theme = YolBi.instance.getModuleManager().getModule(ClientTheme.class);
        AbstractFontRenderer fontRenderer = YolBi.instance.getFontManager().getJelloRegular18();
        fontRenderer.drawString(YolBi.name + " " + YolBi.version, 2, 2, new Color(-1).getRGB());
        List<Module> activeModules = YolBi.instance.getModuleManager().getModules().stream().filter(Module::isEnabled).sorted(Comparator.comparingInt(m -> -(m.getName() + (m.getSuffix() != null ? " " + m.getSuffix() : "")).length())).collect(Collectors.toList());
        for (int i = 0; i < activeModules.size(); i++) {
            Module module = activeModules.get(i);
            String text = module.getName() + (module.getSuffix() != null ? " " + module.getSuffix() : "");
            fontRenderer.drawString(text, e.getScaledresolution().getScaledWidth() - fontRenderer.getStringWidth(text) - 2, 2 + i * (fontRenderer.getStringHeight() + 2), theme.getColor(i));
        }
    }
}
