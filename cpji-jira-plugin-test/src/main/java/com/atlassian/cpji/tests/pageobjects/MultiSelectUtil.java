package com.atlassian.cpji.tests.pageobjects;

import com.atlassian.pageobjects.PageBinder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 *
 * @since v2.1
 */
public class MultiSelectUtil {
	public static void setMultiSelect(@Nonnull PageBinder pageBinder, @Nonnull String id, @Nullable String...values) {
		final List<String> list = values != null ? ImmutableList.<String>builder().add(values).build() : null;
		setMultiSelect(pageBinder, id, list);
	}

	public static void setMultiSelect(@Nonnull PageBinder pageBinder, @Nonnull String id, @Nullable Iterable<String> values)
	{
		final ScriptEngineManager engineManager = new ScriptEngineManager();
		final ScriptEngine scope = engineManager.getEngineByName("nashorn");
		final Bindings bindings = scope.getBindings(ScriptContext.ENGINE_SCOPE);

		bindings.put("items", Lists.newArrayList());
		bindings.put("select", pageBinder.bind(getMultiSelectClass(pageBinder), id));

		try {
			scope.eval("if(select.clear) { select.clear(); } if (select.clearAllItems) { select.clearAllItems(); }");
			if (values != null) {
				for (String value : values) {
					scope.eval("select.add('" + value + "');");
				}
			}
		} catch (ScriptException e) {
			throw new RuntimeException(e);
		}
	}

	public static Iterable<String> getMultiSelect(@Nonnull PageBinder pageBinder, @Nonnull String id) {
		final ScriptEngineManager engineManager = new ScriptEngineManager();
		final ScriptEngine scope = engineManager.getEngineByName("nashorn");
		final Bindings bindings = scope.getBindings(ScriptContext.ENGINE_SCOPE);

		final List<String> items = Lists.newArrayList();
		bindings.put("items", items);
		bindings.put("select", pageBinder.bind(getMultiSelectClass(pageBinder), id));

		try {
			scope.eval("var selected = select.getItems();"
					+ "if (selected.byDefaultTimeout) {"
					+ "selected = selected.byDefaultTimeout().iterator();"
					+ "while(selected.hasNext()) { items.add(selected.next().getName()); }"
					+ "} else {"
					+ "for(var s = selected.size(), i = 0; i<s; ++i) { items.add(selected.get(i).getName()); }"
					+ "}");

			return items;
		} catch (ScriptException e) {
			throw new RuntimeException(e);
		}
	}

	protected static <T> Class getMultiSelectClass(T owner) {
		Class select;
		try {
			select = owner.getClass().getClassLoader().loadClass("com.atlassian.jira.pageobjects.components.MultiSelect");
		} catch (ClassNotFoundException e) {
			try {
				select = owner.getClass().getClassLoader().loadClass("com.atlassian.jira.pageobjects.components.fields.MultiSelect");
			} catch (ClassNotFoundException e1) {
				throw new RuntimeException(e1);
			}
		}
		return select;
	}
}
