package com.nettakrim;

import com.nettakrim.access.AbstractSignEditScreenAccessor;
import net.minecraft.block.entity.SignBlockEntity;

public record SignEditingInfo (SignBlockEntity sign, AbstractSignEditScreenAccessor screen) {}
