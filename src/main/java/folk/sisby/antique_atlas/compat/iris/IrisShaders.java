package folk.sisby.antique_atlas.compat.iris;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class IrisShaders {
	private static final MethodHandle SHADER_PACK_IN_USE = bindProbe();

	private IrisShaders() {
	}

	private static MethodHandle bindProbe() {
		try {
			Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
			MethodHandles.Lookup lookup = MethodHandles.publicLookup();
			Object api = lookup.findStatic(apiClass, "getInstance", MethodType.methodType(apiClass)).invoke();
			return lookup.findVirtual(apiClass, "isShaderPackInUse", MethodType.methodType(boolean.class)).bindTo(api);
		} catch (Throwable e) {
			return null;
		}
	}

	public static boolean inUse() {
		if (SHADER_PACK_IN_USE == null) return false;
		try {
			return (boolean) SHADER_PACK_IN_USE.invokeExact();
		} catch (Throwable e) {
			return false;
		}
	}
}
