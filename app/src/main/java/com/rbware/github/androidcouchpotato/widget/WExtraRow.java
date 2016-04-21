package com.rbware.github.androidcouchpotato.widget;

public class WExtraRow extends Row {

	private Object extra;

	public WExtraRow(HeaderItem headerItem, Object extra) {
		super(headerItem);
		this.extra = extra;
	}

	public Object getExtra() {
		return extra;
	}

}
