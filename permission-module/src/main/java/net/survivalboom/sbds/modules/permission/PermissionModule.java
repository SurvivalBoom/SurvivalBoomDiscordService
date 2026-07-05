package net.survivalboom.sbds.modules.permission;

import net.survivalboom.sbds.api.modules.ModuleMain;
import net.survivalboom.sbds.modules.permission.commands.PermissionCommand;

public class PermissionModule extends ModuleMain {

    @Override
    public void onEnable() {
        addModuleTranslations2(
                "translation_uk.yml",
                "translation_en.yml",
                "translation_ru.yml"
        );

        registerCommand(new PermissionCommand());
    }
}