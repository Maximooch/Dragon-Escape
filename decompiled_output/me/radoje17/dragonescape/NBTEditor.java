package me.radoje17.dragonescape;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class NBTEditor {
   private static final Map<String, Class<?>> classCache = new HashMap<>();
   private static final Map<String, Method> methodCache;
   private static final Map<Class<?>, Constructor<?>> constructorCache;
   private static final Map<Class<?>, Class<?>> NBTClasses;
   private static final Map<Class<?>, Field> NBTTagFieldCache;
   private static Field NBTListData;
   private static Field NBTCompoundMap;
   private static final String VERSION = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
   private static final NBTEditor.MinecraftVersion LOCAL_VERSION = NBTEditor.MinecraftVersion.get(VERSION);

   private static Class<?> getNBTTag(Class<?> primitiveType) {
      return NBTClasses.containsKey(primitiveType) ? NBTClasses.get(primitiveType) : primitiveType;
   }

   private static Object getNBTVar(Object object) {
      if (object == null) {
         return null;
      } else {
         Class<?> clazz = object.getClass();

         try {
            if (NBTTagFieldCache.containsKey(clazz)) {
               return NBTTagFieldCache.get(clazz).get(object);
            }
         } catch (Exception var3) {
            var3.printStackTrace();
         }

         return null;
      }
   }

   private static Method getMethod(String name) {
      return methodCache.containsKey(name) ? methodCache.get(name) : null;
   }

   private static Constructor<?> getConstructor(Class<?> clazz) {
      return constructorCache.containsKey(clazz) ? constructorCache.get(clazz) : null;
   }

   private static Class<?> getNMSClass(String name) {
      if (classCache.containsKey(name)) {
         return classCache.get(name);
      } else {
         try {
            return Class.forName("net.minecraft.server." + VERSION + "." + name);
         } catch (ClassNotFoundException var2) {
            var2.printStackTrace();
            return null;
         }
      }
   }

   private static String getMatch(String string, String regex) {
      Pattern pattern = Pattern.compile(regex);
      Matcher matcher = pattern.matcher(string);
      return matcher.find() ? matcher.group(1) : null;
   }

   private static Object createItemStack(Object compound) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException {
      return LOCAL_VERSION != NBTEditor.MinecraftVersion.v1_11 && LOCAL_VERSION != NBTEditor.MinecraftVersion.v1_12
         ? getMethod("createStack").invoke(null, compound)
         : getConstructor(getNMSClass("ItemStack")).newInstance(compound);
   }

   public static String getVersion() {
      return VERSION;
   }

   public static NBTEditor.MinecraftVersion getMinecraftVersion() {
      return LOCAL_VERSION;
   }

   public static ItemStack getHead(String skinURL) {
      Material material = Material.getMaterial("SKULL_ITEM");
      if (material == null) {
         material = Material.getMaterial("PLAYER_HEAD");
      }

      ItemStack head = new ItemStack(material, 1, (short)3);
      if (skinURL != null && !skinURL.isEmpty()) {
         ItemMeta headMeta = head.getItemMeta();
         Object profile = null;

         try {
            profile = getConstructor(getNMSClass("GameProfile")).newInstance(UUID.randomUUID(), null);
            Object propertyMap = getMethod("getProperties").invoke(profile);
            Object textureProperty = getConstructor(getNMSClass("Property"))
               .newInstance("textures", new String(Base64.getEncoder().encode(String.format("{textures:{SKIN:{\"url\":\"%s\"}}}", skinURL).getBytes())));
            getMethod("put").invoke(propertyMap, "textures", textureProperty);
         } catch (IllegalArgumentException | InvocationTargetException | InstantiationException | IllegalAccessException var10) {
            var10.printStackTrace();
         }

         if (methodCache.containsKey("setProfile")) {
            try {
               getMethod("setProfile").invoke(headMeta, profile);
            } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException var9) {
               var9.printStackTrace();
            }
         } else {
            Field profileField = null;

            try {
               profileField = headMeta.getClass().getDeclaredField("profile");
            } catch (SecurityException | NoSuchFieldException var8) {
               var8.printStackTrace();
            }

            profileField.setAccessible(true);

            try {
               profileField.set(headMeta, profile);
            } catch (IllegalAccessException | IllegalArgumentException var7) {
               var7.printStackTrace();
            }
         }

         head.setItemMeta(headMeta);
         return head;
      } else {
         return head;
      }
   }

   public static String getTexture(ItemStack head) {
      ItemMeta meta = head.getItemMeta();
      Field profileField = null;

      try {
         profileField = meta.getClass().getDeclaredField("profile");
      } catch (SecurityException | NoSuchFieldException var9) {
         var9.printStackTrace();
         throw new IllegalArgumentException("Item is not a player skull!");
      }

      profileField.setAccessible(true);

      try {
         Object profile = profileField.get(meta);
         if (profile == null) {
            return null;
         } else {
            for (Object prop : (Collection)getMethod("values").invoke(getMethod("getProperties").invoke(profile))) {
               if ("textures".equals(getMethod("getName").invoke(prop))) {
                  String texture = new String(Base64.getDecoder().decode((String)getMethod("getValue").invoke(prop)));
                  return getMatch(texture, "\\{\"url\":\"(.*?)\"\\}");
               }
            }

            return null;
         }
      } catch (IllegalAccessException | SecurityException | InvocationTargetException | IllegalArgumentException var8) {
         var8.printStackTrace();
         return null;
      }
   }

   private static Object getItemTag(ItemStack item, Object... keys) {
      try {
         return getTag(getCompound(item), keys);
      } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException var3) {
         var3.printStackTrace();
         return null;
      }
   }

   private static Object getCompound(ItemStack item) {
      if (item == null) {
         return null;
      } else {
         try {
            Object stack = null;
            stack = getMethod("asNMSCopy").invoke(null, item);
            Object tag = null;
            if (getMethod("hasTag").invoke(stack).equals(true)) {
               tag = getMethod("getTag").invoke(stack);
            } else {
               tag = getNMSClass("NBTTagCompound").newInstance();
            }

            return tag;
         } catch (Exception var3) {
            var3.printStackTrace();
            return null;
         }
      }
   }

   private static NBTEditor.NBTCompound getItemNBTTag(ItemStack item, Object... keys) {
      if (item == null) {
         return null;
      } else {
         try {
            Object stack = null;
            stack = getMethod("asNMSCopy").invoke(null, item);
            Object tag = getNMSClass("NBTTagCompound").newInstance();
            tag = getMethod("save").invoke(stack, tag);
            return getNBTTag(tag, keys);
         } catch (Exception var4) {
            var4.printStackTrace();
            return null;
         }
      }
   }

   private static ItemStack setItemTag(ItemStack item, Object value, Object... keys) {
      if (item == null) {
         return null;
      } else {
         try {
            Object stack = getMethod("asNMSCopy").invoke(null, item);
            Object tag = null;
            if (getMethod("hasTag").invoke(stack).equals(true)) {
               tag = getMethod("getTag").invoke(stack);
            } else {
               tag = getNMSClass("NBTTagCompound").newInstance();
            }

            if (keys.length == 0 && value instanceof NBTEditor.NBTCompound) {
               tag = ((NBTEditor.NBTCompound)value).tag;
            } else {
               setTag(tag, value, keys);
            }

            getMethod("setTag").invoke(stack, tag);
            return (ItemStack)getMethod("asBukkitCopy").invoke(null, stack);
         } catch (Exception var5) {
            var5.printStackTrace();
            return null;
         }
      }
   }

   public static ItemStack getItemFromTag(NBTEditor.NBTCompound compound) {
      if (compound == null) {
         return null;
      } else {
         try {
            Object tag = compound.tag;
            Object count = getTag(tag, "Count");
            Object id = getTag(tag, "id");
            if (count == null || id == null) {
               return null;
            } else {
               return count instanceof Byte && id instanceof String ? (ItemStack)getMethod("asBukkitCopy").invoke(null, createItemStack(tag)) : null;
            }
         } catch (Exception var4) {
            var4.printStackTrace();
            return null;
         }
      }
   }

   private static Object getEntityTag(Entity entity, Object... keys) {
      try {
         return getTag(getCompound(entity), keys);
      } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException var3) {
         var3.printStackTrace();
         return null;
      }
   }

   private static Object getCompound(Entity entity) {
      if (entity == null) {
         return entity;
      } else {
         try {
            Object NMSEntity = getMethod("getEntityHandle").invoke(entity);
            Object tag = getNMSClass("NBTTagCompound").newInstance();
            getMethod("getEntityTag").invoke(NMSEntity, tag);
            return tag;
         } catch (Exception var3) {
            var3.printStackTrace();
            return null;
         }
      }
   }

   private static NBTEditor.NBTCompound getEntityNBTTag(Entity entity, Object... keys) {
      if (entity == null) {
         return null;
      } else {
         try {
            Object NMSEntity = getMethod("getEntityHandle").invoke(entity);
            Object tag = getNMSClass("NBTTagCompound").newInstance();
            getMethod("getEntityTag").invoke(NMSEntity, tag);
            return getNBTTag(tag, keys);
         } catch (Exception var4) {
            var4.printStackTrace();
            return null;
         }
      }
   }

   private static void setEntityTag(Entity entity, Object value, Object... keys) {
      if (entity != null) {
         try {
            Object NMSEntity = getMethod("getEntityHandle").invoke(entity);
            Object tag = getNMSClass("NBTTagCompound").newInstance();
            getMethod("getEntityTag").invoke(NMSEntity, tag);
            if (keys.length == 0 && value instanceof NBTEditor.NBTCompound) {
               tag = ((NBTEditor.NBTCompound)value).tag;
            } else {
               setTag(tag, value, keys);
            }

            getMethod("setEntityTag").invoke(NMSEntity, tag);
         } catch (Exception var5) {
            var5.printStackTrace();
         }
      }
   }

   private static Object getBlockTag(Block block, Object... keys) {
      try {
         return getTag(getCompound(block), keys);
      } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException var3) {
         var3.printStackTrace();
         return null;
      }
   }

   private static Object getCompound(Block block) {
      try {
         if (block != null && getNMSClass("CraftBlockState").isInstance(block.getState())) {
            Location location = block.getLocation();
            Object blockPosition = getConstructor(getNMSClass("BlockPosition")).newInstance(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            Object nmsWorld = getMethod("getWorldHandle").invoke(location.getWorld());
            Object tileEntity = getMethod("getTileEntity").invoke(nmsWorld, blockPosition);
            Object tag = getNMSClass("NBTTagCompound").newInstance();
            getMethod("getTileTag").invoke(tileEntity, tag);
            return tag;
         } else {
            return null;
         }
      } catch (Exception var6) {
         var6.printStackTrace();
         return null;
      }
   }

   private static NBTEditor.NBTCompound getBlockNBTTag(Block block, Object... keys) {
      try {
         if (block != null && getNMSClass("CraftBlockState").isInstance(block.getState())) {
            Location location = block.getLocation();
            Object blockPosition = getConstructor(getNMSClass("BlockPosition")).newInstance(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            Object nmsWorld = getMethod("getWorldHandle").invoke(location.getWorld());
            Object tileEntity = getMethod("getTileEntity").invoke(nmsWorld, blockPosition);
            Object tag = getNMSClass("NBTTagCompound").newInstance();
            getMethod("getTileTag").invoke(tileEntity, tag);
            return getNBTTag(tag, keys);
         } else {
            return null;
         }
      } catch (Exception var7) {
         var7.printStackTrace();
         return null;
      }
   }

   private static void setBlockTag(Block block, Object value, Object... keys) {
      try {
         if (block != null && getNMSClass("CraftBlockState").isInstance(block.getState())) {
            Location location = block.getLocation();
            Object blockPosition = getConstructor(getNMSClass("BlockPosition")).newInstance(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            Object nmsWorld = getMethod("getWorldHandle").invoke(location.getWorld());
            Object tileEntity = getMethod("getTileEntity").invoke(nmsWorld, blockPosition);
            Object tag = getNMSClass("NBTTagCompound").newInstance();
            getMethod("getTileTag").invoke(tileEntity, tag);
            if (keys.length == 0 && value instanceof NBTEditor.NBTCompound) {
               tag = ((NBTEditor.NBTCompound)value).tag;
            } else {
               setTag(tag, value, keys);
            }

            if (LOCAL_VERSION.greaterThanOrEqualTo(NBTEditor.MinecraftVersion.v1_16)) {
               getMethod("setTileTag").invoke(tileEntity, getMethod("getType").invoke(nmsWorld, blockPosition), tag);
            } else {
               getMethod("setTileTag").invoke(tileEntity, tag);
            }
         }
      } catch (Exception var8) {
         var8.printStackTrace();
      }
   }

   public static void setSkullTexture(Block block, String texture) {
      try {
         Object profile = getConstructor(getNMSClass("GameProfile")).newInstance(UUID.randomUUID(), null);
         Object propertyMap = getMethod("getProperties").invoke(profile);
         Object textureProperty = getConstructor(getNMSClass("Property"))
            .newInstance("textures", new String(Base64.getEncoder().encode(String.format("{textures:{SKIN:{\"url\":\"%s\"}}}", texture).getBytes())));
         getMethod("put").invoke(propertyMap, "textures", textureProperty);
         Location location = block.getLocation();
         Object blockPosition = getConstructor(getNMSClass("BlockPosition")).newInstance(location.getBlockX(), location.getBlockY(), location.getBlockZ());
         Object nmsWorld = getMethod("getWorldHandle").invoke(location.getWorld());
         Object tileEntity = getMethod("getTileEntity").invoke(nmsWorld, blockPosition);
         getMethod("setGameProfile").invoke(tileEntity, profile);
      } catch (Exception var9) {
         var9.printStackTrace();
      }
   }

   private static Object getValue(Object object, Object... keys) {
      if (object instanceof ItemStack) {
         return getItemTag((ItemStack)object, keys);
      } else if (object instanceof Entity) {
         return getEntityTag((Entity)object, keys);
      } else if (object instanceof Block) {
         return getBlockTag((Block)object, keys);
      } else if (object instanceof NBTEditor.NBTCompound) {
         try {
            return getTag(((NBTEditor.NBTCompound)object).tag, keys);
         } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException var3) {
            var3.printStackTrace();
            return null;
         }
      } else {
         throw new IllegalArgumentException("Object provided must be of type ItemStack, Entity, Block, or NBTCompound!");
      }
   }

   public static NBTEditor.NBTCompound getNBTCompound(Object object, Object... keys) {
      if (object instanceof ItemStack) {
         return getItemNBTTag((ItemStack)object, keys);
      } else if (object instanceof Entity) {
         return getEntityNBTTag((Entity)object, keys);
      } else if (object instanceof Block) {
         return getBlockNBTTag((Block)object, keys);
      } else if (object instanceof NBTEditor.NBTCompound) {
         try {
            return getNBTTag(((NBTEditor.NBTCompound)object).tag, keys);
         } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException var3) {
            var3.printStackTrace();
            return null;
         }
      } else if (getNMSClass("NBTTagCompound").isInstance(object)) {
         try {
            return getNBTTag(object, keys);
         } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException var4) {
            var4.printStackTrace();
            return null;
         }
      } else {
         throw new IllegalArgumentException("Object provided must be of type ItemStack, Entity, Block, or NBTCompound!");
      }
   }

   public static String getString(Object object, Object... keys) {
      Object result = getValue(object, keys);
      return result instanceof String ? (String)result : null;
   }

   public static int getInt(Object object, Object... keys) {
      Object result = getValue(object, keys);
      return result instanceof Integer ? (Integer)result : 0;
   }

   public static double getDouble(Object object, Object... keys) {
      Object result = getValue(object, keys);
      return result instanceof Double ? (Double)result : 0.0;
   }

   public static long getLong(Object object, Object... keys) {
      Object result = getValue(object, keys);
      return result instanceof Long ? (Long)result : 0L;
   }

   public static float getFloat(Object object, Object... keys) {
      Object result = getValue(object, keys);
      return result instanceof Float ? (Float)result : 0.0F;
   }

   public static short getShort(Object object, Object... keys) {
      Object result = getValue(object, keys);
      return result instanceof Short ? (Short)result : 0;
   }

   public static byte getByte(Object object, Object... keys) {
      Object result = getValue(object, keys);
      return result instanceof Byte ? (Byte)result : 0;
   }

   public static boolean getBoolean(Object object, Object... keys) {
      return getByte(object, keys) == 1;
   }

   public static byte[] getByteArray(Object object, Object... keys) {
      Object result = getValue(object, keys);
      return result instanceof byte[] ? (byte[])result : null;
   }

   public static int[] getIntArray(Object object, Object... keys) {
      Object result = getValue(object, keys);
      return result instanceof int[] ? (int[])result : null;
   }

   public static boolean contains(Object object, Object... keys) {
      Object result = getValue(object, keys);
      return result != null;
   }

   public static Collection<String> getKeys(Object object, Object... keys) {
      Object compound;
      if (object instanceof ItemStack) {
         compound = getCompound((ItemStack)object);
      } else if (object instanceof Entity) {
         compound = getCompound((Entity)object);
      } else if (object instanceof Block) {
         compound = getCompound((Block)object);
      } else {
         if (!(object instanceof NBTEditor.NBTCompound)) {
            throw new IllegalArgumentException("Object provided must be of type ItemStack, Entity, Block, or NBTCompound!");
         }

         compound = ((NBTEditor.NBTCompound)object).tag;
      }

      try {
         NBTEditor.NBTCompound nbtCompound = getNBTTag(compound, keys);
         Object tag = nbtCompound.tag;
         return getNMSClass("NBTTagCompound").isInstance(tag) ? (Collection)getMethod("getKeys").invoke(tag) : null;
      } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException var5) {
         var5.printStackTrace();
         return null;
      }
   }

   public static int getSize(Object object, Object... keys) {
      Object compound;
      if (object instanceof ItemStack) {
         compound = getCompound((ItemStack)object);
      } else if (object instanceof Entity) {
         compound = getCompound((Entity)object);
      } else if (object instanceof Block) {
         compound = getCompound((Block)object);
      } else {
         if (!(object instanceof NBTEditor.NBTCompound)) {
            throw new IllegalArgumentException("Object provided must be of type ItemStack, Entity, Block, or NBTCompound!");
         }

         compound = ((NBTEditor.NBTCompound)object).tag;
      }

      try {
         NBTEditor.NBTCompound nbtCompound = getNBTTag(compound, keys);
         if (getNMSClass("NBTTagCompound").isInstance(nbtCompound.tag)) {
            return getKeys(nbtCompound).size();
         }

         if (getNMSClass("NBTTagList").isInstance(nbtCompound.tag)) {
            return (Integer)getMethod("size").invoke(nbtCompound.tag);
         }
      } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException var4) {
         var4.printStackTrace();
         return 0;
      }

      throw new IllegalArgumentException("Value is not a compound or list!");
   }

   public static <T> T set(T object, Object value, Object... keys) {
      if (object instanceof ItemStack) {
         return (T)setItemTag((ItemStack)object, value, keys);
      } else {
         if (object instanceof Entity) {
            setEntityTag((Entity)object, value, keys);
         } else if (object instanceof Block) {
            setBlockTag((Block)object, value, keys);
         } else {
            if (!(object instanceof NBTEditor.NBTCompound)) {
               throw new IllegalArgumentException("Object provided must be of type ItemStack, Entity, Block, or NBTCompound!");
            }

            try {
               setTag(((NBTEditor.NBTCompound)object).tag, value, keys);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | InstantiationException var4) {
               var4.printStackTrace();
            }
         }

         return object;
      }
   }

   public static NBTEditor.NBTCompound getNBTCompound(String json) {
      return NBTEditor.NBTCompound.fromJson(json);
   }

   public static NBTEditor.NBTCompound getEmptyNBTCompound() {
      try {
         return new NBTEditor.NBTCompound(getNMSClass("NBTTagCompound").newInstance());
      } catch (IllegalAccessException | InstantiationException var1) {
         var1.printStackTrace();
         return null;
      }
   }

   private static void setTag(Object tag, Object value, Object... keys) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
      Object notCompound;
      if (value != null) {
         if (value instanceof NBTEditor.NBTCompound) {
            notCompound = ((NBTEditor.NBTCompound)value).tag;
         } else if (!getNMSClass("NBTTagList").isInstance(value) && !getNMSClass("NBTTagCompound").isInstance(value)) {
            if (value instanceof Boolean) {
               value = (byte)((Boolean)value ? 1 : 0);
            }

            notCompound = getConstructor(getNBTTag(value.getClass())).newInstance(value);
         } else {
            notCompound = value;
         }
      } else {
         notCompound = null;
      }

      Object compound = tag;

      for (int index = 0; index < keys.length - 1; index++) {
         Object key = keys[index];
         Object oldCompound = compound;
         if (key instanceof Integer) {
            compound = ((List)NBTListData.get(compound)).get((Integer)key);
         } else if (key != null) {
            compound = getMethod("get").invoke(compound, (String)key);
         }

         if (compound == null || key == null) {
            if (keys[index + 1] != null && !(keys[index + 1] instanceof Integer)) {
               compound = getNMSClass("NBTTagCompound").newInstance();
            } else {
               compound = getNMSClass("NBTTagList").newInstance();
            }

            if (oldCompound.getClass().getSimpleName().equals("NBTTagList")) {
               if (LOCAL_VERSION.greaterThanOrEqualTo(NBTEditor.MinecraftVersion.v1_14)) {
                  getMethod("add").invoke(oldCompound, getMethod("size").invoke(oldCompound), compound);
               } else {
                  getMethod("add").invoke(oldCompound, compound);
               }
            } else {
               getMethod("set").invoke(oldCompound, (String)key, compound);
            }
         }
      }

      if (keys.length > 0) {
         Object lastKey = keys[keys.length - 1];
         if (lastKey == null) {
            if (LOCAL_VERSION.greaterThanOrEqualTo(NBTEditor.MinecraftVersion.v1_14)) {
               getMethod("add").invoke(compound, getMethod("size").invoke(compound), notCompound);
            } else {
               getMethod("add").invoke(compound, notCompound);
            }
         } else if (lastKey instanceof Integer) {
            if (notCompound == null) {
               getMethod("listRemove").invoke(compound, (Integer)lastKey);
            } else {
               getMethod("setIndex").invoke(compound, (Integer)lastKey, notCompound);
            }
         } else if (notCompound == null) {
            getMethod("remove").invoke(compound, (String)lastKey);
         } else {
            getMethod("set").invoke(compound, (String)lastKey, notCompound);
         }
      } else if (notCompound != null) {
      }
   }

   private static NBTEditor.NBTCompound getNBTTag(Object tag, Object... keys) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
      Object compound = tag;

      for (Object key : keys) {
         if (compound == null) {
            return null;
         }

         if (getNMSClass("NBTTagCompound").isInstance(compound)) {
            compound = getMethod("get").invoke(compound, (String)key);
         } else if (getNMSClass("NBTTagList").isInstance(compound)) {
            compound = ((List)NBTListData.get(compound)).get((Integer)key);
         }
      }

      return new NBTEditor.NBTCompound(compound);
   }

   private static Object getTag(Object tag, Object... keys) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
      if (keys.length == 0) {
         return getTags(tag);
      } else {
         Object notCompound = tag;

         for (Object key : keys) {
            if (notCompound == null) {
               return null;
            }

            if (getNMSClass("NBTTagCompound").isInstance(notCompound)) {
               notCompound = getMethod("get").invoke(notCompound, (String)key);
            } else {
               if (!getNMSClass("NBTTagList").isInstance(notCompound)) {
                  return getNBTVar(notCompound);
               }

               notCompound = ((List)NBTListData.get(notCompound)).get((Integer)key);
            }
         }

         if (notCompound == null) {
            return null;
         } else if (getNMSClass("NBTTagList").isInstance(notCompound)) {
            return getTags(notCompound);
         } else {
            return getNMSClass("NBTTagCompound").isInstance(notCompound) ? getTags(notCompound) : getNBTVar(notCompound);
         }
      }
   }

   private static Object getTags(Object tag) {
      Map<Object, Object> tags = new HashMap<>();

      try {
         if (getNMSClass("NBTTagCompound").isInstance(tag)) {
            Map<String, Object> tagCompound = (Map<String, Object>)NBTCompoundMap.get(tag);

            for (String key : tagCompound.keySet()) {
               Object value = tagCompound.get(key);
               if (!getNMSClass("NBTTagEnd").isInstance(value)) {
                  tags.put(key, getTag(value));
               }
            }
         } else {
            if (!getNMSClass("NBTTagList").isInstance(tag)) {
               return getNBTVar(tag);
            }

            List<Object> tagList = (List<Object>)NBTListData.get(tag);

            for (int index = 0; index < tagList.size(); index++) {
               Object value = tagList.get(index);
               if (!getNMSClass("NBTTagEnd").isInstance(value)) {
                  tags.put(index, getTag(value));
               }
            }
         }

         return tags;
      } catch (Exception var6) {
         var6.printStackTrace();
         return tags;
      }
   }

   static {
      try {
         classCache.put("NBTBase", Class.forName("net.minecraft.server." + VERSION + ".NBTBase"));
         classCache.put("NBTTagCompound", Class.forName("net.minecraft.server." + VERSION + ".NBTTagCompound"));
         classCache.put("NBTTagList", Class.forName("net.minecraft.server." + VERSION + ".NBTTagList"));
         classCache.put("MojangsonParser", Class.forName("net.minecraft.server." + VERSION + ".MojangsonParser"));
         classCache.put("ItemStack", Class.forName("net.minecraft.server." + VERSION + ".ItemStack"));
         classCache.put("CraftItemStack", Class.forName("org.bukkit.craftbukkit." + VERSION + ".inventory.CraftItemStack"));
         classCache.put("CraftMetaSkull", Class.forName("org.bukkit.craftbukkit." + VERSION + ".inventory.CraftMetaSkull"));
         classCache.put("Entity", Class.forName("net.minecraft.server." + VERSION + ".Entity"));
         classCache.put("CraftEntity", Class.forName("org.bukkit.craftbukkit." + VERSION + ".entity.CraftEntity"));
         classCache.put("EntityLiving", Class.forName("net.minecraft.server." + VERSION + ".EntityLiving"));
         classCache.put("CraftWorld", Class.forName("org.bukkit.craftbukkit." + VERSION + ".CraftWorld"));
         classCache.put("CraftBlockState", Class.forName("org.bukkit.craftbukkit." + VERSION + ".block.CraftBlockState"));
         classCache.put("BlockPosition", Class.forName("net.minecraft.server." + VERSION + ".BlockPosition"));
         classCache.put("TileEntity", Class.forName("net.minecraft.server." + VERSION + ".TileEntity"));
         classCache.put("World", Class.forName("net.minecraft.server." + VERSION + ".World"));
         classCache.put("IBlockData", Class.forName("net.minecraft.server." + VERSION + ".IBlockData"));
         classCache.put("TileEntitySkull", Class.forName("net.minecraft.server." + VERSION + ".TileEntitySkull"));
         classCache.put("GameProfile", Class.forName("com.mojang.authlib.GameProfile"));
         classCache.put("Property", Class.forName("com.mojang.authlib.properties.Property"));
         classCache.put("PropertyMap", Class.forName("com.mojang.authlib.properties.PropertyMap"));
      } catch (ClassNotFoundException var10) {
         var10.printStackTrace();
      }

      NBTClasses = new HashMap<>();

      try {
         NBTClasses.put(Byte.class, Class.forName("net.minecraft.server." + VERSION + ".NBTTagByte"));
         NBTClasses.put(Boolean.class, Class.forName("net.minecraft.server." + VERSION + ".NBTTagByte"));
         NBTClasses.put(String.class, Class.forName("net.minecraft.server." + VERSION + ".NBTTagString"));
         NBTClasses.put(Double.class, Class.forName("net.minecraft.server." + VERSION + ".NBTTagDouble"));
         NBTClasses.put(Integer.class, Class.forName("net.minecraft.server." + VERSION + ".NBTTagInt"));
         NBTClasses.put(Long.class, Class.forName("net.minecraft.server." + VERSION + ".NBTTagLong"));
         NBTClasses.put(Short.class, Class.forName("net.minecraft.server." + VERSION + ".NBTTagShort"));
         NBTClasses.put(Float.class, Class.forName("net.minecraft.server." + VERSION + ".NBTTagFloat"));
         NBTClasses.put(Class.forName("[B"), Class.forName("net.minecraft.server." + VERSION + ".NBTTagByteArray"));
         NBTClasses.put(Class.forName("[I"), Class.forName("net.minecraft.server." + VERSION + ".NBTTagIntArray"));
      } catch (ClassNotFoundException var9) {
         var9.printStackTrace();
      }

      methodCache = new HashMap<>();

      try {
         methodCache.put("get", getNMSClass("NBTTagCompound").getMethod("get", String.class));
         methodCache.put("set", getNMSClass("NBTTagCompound").getMethod("set", String.class, getNMSClass("NBTBase")));
         methodCache.put("hasKey", getNMSClass("NBTTagCompound").getMethod("hasKey", String.class));
         methodCache.put("setIndex", getNMSClass("NBTTagList").getMethod("a", int.class, getNMSClass("NBTBase")));
         if (LOCAL_VERSION.greaterThanOrEqualTo(NBTEditor.MinecraftVersion.v1_14)) {
            methodCache.put("getTypeId", getNMSClass("NBTBase").getMethod("getTypeId"));
            methodCache.put("add", getNMSClass("NBTTagList").getMethod("add", int.class, getNMSClass("NBTBase")));
         } else {
            methodCache.put("add", getNMSClass("NBTTagList").getMethod("add", getNMSClass("NBTBase")));
         }

         methodCache.put("size", getNMSClass("NBTTagList").getMethod("size"));
         if (LOCAL_VERSION == NBTEditor.MinecraftVersion.v1_8) {
            methodCache.put("listRemove", getNMSClass("NBTTagList").getMethod("a", int.class));
         } else {
            methodCache.put("listRemove", getNMSClass("NBTTagList").getMethod("remove", int.class));
         }

         methodCache.put("remove", getNMSClass("NBTTagCompound").getMethod("remove", String.class));
         if (LOCAL_VERSION.greaterThanOrEqualTo(NBTEditor.MinecraftVersion.v1_13)) {
            methodCache.put("getKeys", getNMSClass("NBTTagCompound").getMethod("getKeys"));
         } else {
            methodCache.put("getKeys", getNMSClass("NBTTagCompound").getMethod("c"));
         }

         methodCache.put("hasTag", getNMSClass("ItemStack").getMethod("hasTag"));
         methodCache.put("getTag", getNMSClass("ItemStack").getMethod("getTag"));
         methodCache.put("setTag", getNMSClass("ItemStack").getMethod("setTag", getNMSClass("NBTTagCompound")));
         methodCache.put("asNMSCopy", getNMSClass("CraftItemStack").getMethod("asNMSCopy", ItemStack.class));
         methodCache.put("asBukkitCopy", getNMSClass("CraftItemStack").getMethod("asBukkitCopy", getNMSClass("ItemStack")));
         methodCache.put("getEntityHandle", getNMSClass("CraftEntity").getMethod("getHandle"));
         if (LOCAL_VERSION.greaterThanOrEqualTo(NBTEditor.MinecraftVersion.v1_16)) {
            methodCache.put("getEntityTag", getNMSClass("Entity").getMethod("save", getNMSClass("NBTTagCompound")));
            methodCache.put("setEntityTag", getNMSClass("Entity").getMethod("load", getNMSClass("NBTTagCompound")));
         } else {
            methodCache.put("getEntityTag", getNMSClass("Entity").getMethod("c", getNMSClass("NBTTagCompound")));
            methodCache.put("setEntityTag", getNMSClass("Entity").getMethod("f", getNMSClass("NBTTagCompound")));
         }

         methodCache.put("save", getNMSClass("ItemStack").getMethod("save", getNMSClass("NBTTagCompound")));
         if (LOCAL_VERSION.lessThanOrEqualTo(NBTEditor.MinecraftVersion.v1_10)) {
            methodCache.put("createStack", getNMSClass("ItemStack").getMethod("createStack", getNMSClass("NBTTagCompound")));
         } else if (LOCAL_VERSION.greaterThanOrEqualTo(NBTEditor.MinecraftVersion.v1_13)) {
            methodCache.put("createStack", getNMSClass("ItemStack").getMethod("a", getNMSClass("NBTTagCompound")));
         }

         if (LOCAL_VERSION.greaterThanOrEqualTo(NBTEditor.MinecraftVersion.v1_16)) {
            methodCache.put("setTileTag", getNMSClass("TileEntity").getMethod("load", getNMSClass("IBlockData"), getNMSClass("NBTTagCompound")));
            methodCache.put("getType", getNMSClass("World").getMethod("getType", getNMSClass("BlockPosition")));
         } else if (LOCAL_VERSION.greaterThanOrEqualTo(NBTEditor.MinecraftVersion.v1_12)) {
            methodCache.put("setTileTag", getNMSClass("TileEntity").getMethod("load", getNMSClass("NBTTagCompound")));
         } else {
            methodCache.put("setTileTag", getNMSClass("TileEntity").getMethod("a", getNMSClass("NBTTagCompound")));
         }

         methodCache.put("getTileEntity", getNMSClass("World").getMethod("getTileEntity", getNMSClass("BlockPosition")));
         methodCache.put("getWorldHandle", getNMSClass("CraftWorld").getMethod("getHandle"));
         methodCache.put("setGameProfile", getNMSClass("TileEntitySkull").getMethod("setGameProfile", getNMSClass("GameProfile")));
         methodCache.put("getProperties", getNMSClass("GameProfile").getMethod("getProperties"));
         methodCache.put("getName", getNMSClass("Property").getMethod("getName"));
         methodCache.put("getValue", getNMSClass("Property").getMethod("getValue"));
         methodCache.put("values", getNMSClass("PropertyMap").getMethod("values"));
         methodCache.put("put", getNMSClass("PropertyMap").getMethod("put", Object.class, Object.class));
         methodCache.put("loadNBTTagCompound", getNMSClass("MojangsonParser").getMethod("parse", String.class));
      } catch (Exception var8) {
         var8.printStackTrace();
      }

      try {
         methodCache.put("getTileTag", getNMSClass("TileEntity").getMethod("save", getNMSClass("NBTTagCompound")));
      } catch (NoSuchMethodException var6) {
         try {
            methodCache.put("getTileTag", getNMSClass("TileEntity").getMethod("b", getNMSClass("NBTTagCompound")));
         } catch (Exception var5) {
            var5.printStackTrace();
         }
      } catch (Exception var7) {
         var7.printStackTrace();
      }

      try {
         methodCache.put("setProfile", getNMSClass("CraftMetaSkull").getDeclaredMethod("setProfile", getNMSClass("GameProfile")));
         methodCache.get("setProfile").setAccessible(true);
      } catch (NoSuchMethodException var4) {
      }

      constructorCache = new HashMap<>();

      try {
         constructorCache.put(getNBTTag(Byte.class), getNBTTag(Byte.class).getDeclaredConstructor(byte.class));
         constructorCache.put(getNBTTag(Boolean.class), getNBTTag(Boolean.class).getDeclaredConstructor(byte.class));
         constructorCache.put(getNBTTag(String.class), getNBTTag(String.class).getDeclaredConstructor(String.class));
         constructorCache.put(getNBTTag(Double.class), getNBTTag(Double.class).getDeclaredConstructor(double.class));
         constructorCache.put(getNBTTag(Integer.class), getNBTTag(Integer.class).getDeclaredConstructor(int.class));
         constructorCache.put(getNBTTag(Long.class), getNBTTag(Long.class).getDeclaredConstructor(long.class));
         constructorCache.put(getNBTTag(Float.class), getNBTTag(Float.class).getDeclaredConstructor(float.class));
         constructorCache.put(getNBTTag(Short.class), getNBTTag(Short.class).getDeclaredConstructor(short.class));
         constructorCache.put(getNBTTag(Class.forName("[B")), getNBTTag(Class.forName("[B")).getDeclaredConstructor(Class.forName("[B")));
         constructorCache.put(getNBTTag(Class.forName("[I")), getNBTTag(Class.forName("[I")).getDeclaredConstructor(Class.forName("[I")));

         for (Constructor<?> cons : constructorCache.values()) {
            cons.setAccessible(true);
         }

         constructorCache.put(getNMSClass("BlockPosition"), getNMSClass("BlockPosition").getConstructor(int.class, int.class, int.class));
         constructorCache.put(getNMSClass("GameProfile"), getNMSClass("GameProfile").getConstructor(UUID.class, String.class));
         constructorCache.put(getNMSClass("Property"), getNMSClass("Property").getConstructor(String.class, String.class));
         if (LOCAL_VERSION == NBTEditor.MinecraftVersion.v1_11 || LOCAL_VERSION == NBTEditor.MinecraftVersion.v1_12) {
            constructorCache.put(getNMSClass("ItemStack"), getNMSClass("ItemStack").getConstructor(getNMSClass("NBTTagCompound")));
         }
      } catch (Exception var12) {
         var12.printStackTrace();
      }

      NBTTagFieldCache = new HashMap<>();

      try {
         for (Class<?> clazz : NBTClasses.values()) {
            Field data = clazz.getDeclaredField("data");
            data.setAccessible(true);
            NBTTagFieldCache.put(clazz, data);
         }
      } catch (Exception var11) {
         var11.printStackTrace();
      }

      try {
         NBTListData = getNMSClass("NBTTagList").getDeclaredField("list");
         NBTListData.setAccessible(true);
         NBTCompoundMap = getNMSClass("NBTTagCompound").getDeclaredField("map");
         NBTCompoundMap.setAccessible(true);
      } catch (Exception var3) {
         var3.printStackTrace();
      }
   }

   public static enum MinecraftVersion {
      v1_8("1_8", 0),
      v1_9("1_9", 1),
      v1_10("1_10", 2),
      v1_11("1_11", 3),
      v1_12("1_12", 4),
      v1_13("1_13", 5),
      v1_14("1_14", 6),
      v1_15("1_15", 7),
      v1_16("1_16", 8),
      v1_17("1_17", 9),
      v1_18("1_18", 10),
      v1_19("1_19", 11);

      private int order;
      private String key;

      private MinecraftVersion(String key, int v) {
         this.key = key;
         this.order = v;
      }

      public boolean greaterThanOrEqualTo(NBTEditor.MinecraftVersion other) {
         return this.order >= other.order;
      }

      public boolean lessThanOrEqualTo(NBTEditor.MinecraftVersion other) {
         return this.order <= other.order;
      }

      public static NBTEditor.MinecraftVersion get(String v) {
         for (NBTEditor.MinecraftVersion k : values()) {
            if (v.contains(k.key)) {
               return k;
            }
         }

         return null;
      }
   }

   public static final class NBTCompound {
      protected final Object tag;

      protected NBTCompound(@Nonnull Object tag) {
         this.tag = tag;
      }

      public void set(Object value, Object... keys) {
         try {
            NBTEditor.setTag(this.tag, value, keys);
         } catch (Exception var4) {
            var4.printStackTrace();
         }
      }

      public String toJson() {
         return this.tag.toString();
      }

      public static NBTEditor.NBTCompound fromJson(String json) {
         try {
            return new NBTEditor.NBTCompound(NBTEditor.getMethod("loadNBTTagCompound").invoke(null, json));
         } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException var2) {
            var2.printStackTrace();
            return null;
         }
      }

      @Override
      public String toString() {
         return this.tag.toString();
      }

      @Override
      public int hashCode() {
         return this.tag.hashCode();
      }

      @Override
      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         } else if (obj == null) {
            return false;
         } else if (this.getClass() != obj.getClass()) {
            return false;
         } else {
            NBTEditor.NBTCompound other = (NBTEditor.NBTCompound)obj;
            if (this.tag == null) {
               if (other.tag != null) {
                  return false;
               }
            } else if (!this.tag.equals(other.tag)) {
               return false;
            }

            return true;
         }
      }
   }
}
