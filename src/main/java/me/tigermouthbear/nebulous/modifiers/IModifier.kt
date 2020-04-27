package me.tigermouthbear.nebulous.modifiers

import me.tigermouthbear.nebulous.Nebulous
import me.tigermouthbear.nebulous.util.Utils
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.SimpleRemapper
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import java.util.jar.Manifest
import java.util.stream.Collectors

/**
 * @author Tigermouthbear
 */

interface IModifier: Utils {
	val classMap: MutableMap<String, ClassNode>
		get() = Nebulous.getClassNodes()

	val classes: List<ClassNode>
		get() = ArrayList(classMap.values)

	val filesMap: MutableMap<String, ByteArray>
		get() = Nebulous.getFiles()

	val manifest: Manifest
		get() = Nebulous.getManifest()

	fun modify()

	fun getName(): String

	fun applyRemap(remap: Map<String?, String?>?) {
		val remapper = SimpleRemapper(remap)
		for(node in classes) {
			val copy = ClassNode()
			val adapter = ClassRemapper(copy, remapper)
			node.accept(adapter)
			classMap.remove(node.name)
			classMap[node.name] = copy
		}
	}

	fun getImplementations(target: ClassNode): List<ClassNode> {
		return classes.stream().filter { cn -> cn.interfaces.contains(target.name) }.collect(Collectors.toList())
	}

	fun getExtensions(target: ClassNode): List<ClassNode> {
		val extensions: MutableList<ClassNode> = mutableListOf()

		classes.stream()
				.filter { cn -> cn.superName == target.name }
				.forEach {cn ->
					extensions.add(cn)
					extensions.addAll(getExtensions(cn))
				}

		return extensions
	}

	fun isDependency(name: String): Boolean {
		val path = getPath(name)
		for(depencency in Nebulous.getDependencies()) {
			if(path.contains(depencency)) return true
		}
		return false
	}

	private fun getPath(name: String): String {
		if(!name.contains("/")) return ""
		val reversedString = reverseString(name)
		val path = reversedString.substring(reversedString.indexOf("/"))
		return reverseString(path)
	}

	private fun reverseString(string: String): String {
		val sb = StringBuilder()
		val chars = string.toCharArray()
		for(i in chars.indices.reversed()) sb.append(chars[i])
		return sb.toString()
	}

	fun getMethod(cn: ClassNode, method: String): MethodNode? {
		cn.methods.forEach { mn ->
			if(mn.name == method) return mn
		}

		return null
	}
}