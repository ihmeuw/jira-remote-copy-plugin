package com.atlassian.cpji.tests.pageobjects;

import com.atlassian.pageobjects.PageBinder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 *
 * @since v2.1
 */
public class MultiSelectUtil {
	public static void setMultiSelect(@Nonnull PageBinder pageBinder, @Nonnull String id, @Nullable String...values) {
		final List<String> list = values != null ? ImmutableList.<String>builder().add(values).build() : null;
		setMultiSelect(pageBinder, id, list);
	}

	public static void setMultiSelect(@Nonnull PageBinder pageBinder, @Nonnull String id, @Nullable Iterable<String> values) {
		final Context cx = Context.enter();
		try {
			final Scriptable scope = cx.initStandardObjects();
			final List<String> items = Lists.newArrayList();
			scope.put("items", scope, items);
			scope.put("select", scope, pageBinder.bind(getMultiSelectClass(pageBinder), id));

			cx.evaluateString(scope, "if(select.clear) { select.clear(); } if (select.clearAllItems) { select.clearAllItems(); }", "js", 1, null);
			if (values != null) {
				for (String value : values) {
					cx.evaluateString(scope, "select.add('" + value + "');", "js", 1, null);
				}
			}
		} finally {
			cx.exit();
		}
	}

	public static Iterable<String> getMultiSelect(@Nonnull PageBinder pageBinder, @Nonnull String id) {
		final Context cx = Context.enter();
		try {
			final Scriptable scope = cx.initStandardObjects();
			final List<String> items = Lists.newArrayList();
			scope.put("items", scope, items);
			scope.put("select", scope, pageBinder.bind(getMultiSelectClass(pageBinder), id));

			cx.evaluateString(scope, "var selected = select.getItems();"
					+ "if (selected.byDefaultTimeout) {"
					+ "selected = selected.byDefaultTimeout().iterator();"
					+ "while(selected.hasNext()) { items.add(selected.next().getName()); }"
					+ "} else {"
					+ "for(var s = selected.size(), i = 0; i<s; ++i) { items.add(selected.get(i).getName()); }"
					+ "}", "js", 1, null);

			return items;
		} finally {
			cx.exit();
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
