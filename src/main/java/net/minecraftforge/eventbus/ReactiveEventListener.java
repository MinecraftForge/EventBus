/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus;

import java.lang.reflect.Type;

import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventListener;
import net.minecraftforge.eventbus.api.IGenericEvent;

class ReactiveEventListener {
	static IEventListener of(IEventListener listener, String readable, Type filter, boolean receiveCancelled, boolean forceCancelable) {
		if (filter == null) {
			if (receiveCancelled)
				return listener;
			if (forceCancelable)
				return new Cancelable(listener, readable);
			return new Unchecked(listener, readable);
		} else {
			if (receiveCancelled)
				return new Generic(listener, readable, filter);
			if (forceCancelable)
				return new GenericCancelable(listener, readable, filter);
			return new GenericReactive(listener, readable, filter);
		}
	}

	private static abstract class Base implements IEventListener {
		protected final IEventListener listener;
		protected final String readable;

		Base(IEventListener listener, String readable) {
			this.listener = listener;
			this.readable = readable;
		}

		@Override
		public String toString() {
			return readable;
		}
	}

	private static class Unchecked extends Base implements IReactiveEventListener {
		Unchecked(IEventListener listener, String readable) {
			super(listener, readable);
		}

		@Override
		public void invoke(Event event) {
			listener.invoke(event);
		}

		@Override
		public IEventListener toCancelable() {
			return new Cancelable(listener, readable);
		}
	}

	private static class Cancelable extends Base {
		Cancelable(IEventListener listener, String readable) {
			super(listener, readable);
		}

		@Override
		public void invoke(Event event) {
			if (!event.isCanceled())
				listener.invoke(event);
		}
	}

	private static class Generic extends Base {
		private final Type filter;

		Generic(IEventListener listener, String readable, Type filter) {
			super(listener, readable);
			this.filter = filter;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public void invoke(Event event) {
            if (this.filter == ((IGenericEvent)event).getGenericType())
            	listener.invoke(event);
		}
	}

	private static class GenericReactive extends Base implements IReactiveEventListener {
		private final Type filter;

		GenericReactive(IEventListener listener, String readable, Type filter) {
			super(listener, readable);
			this.filter = filter;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public void invoke(Event event) {
            if (this.filter == ((IGenericEvent)event).getGenericType())
            	listener.invoke(event);
		}

		@Override
		public IEventListener toCancelable() {
			return new GenericCancelable(listener, readable, filter);
		}
	}

	private static class GenericCancelable extends Base {
		private final Type filter;

		GenericCancelable(IEventListener listener, String readable, Type filter) {
			super(listener, readable);
			this.filter = filter;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public void invoke(Event event) {
            if (!event.isCanceled() && this.filter == ((IGenericEvent)event).getGenericType())
            	listener.invoke(event);
		}
	}
}
