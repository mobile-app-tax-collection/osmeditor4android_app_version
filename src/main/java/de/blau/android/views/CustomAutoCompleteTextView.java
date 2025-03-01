package de.blau.android.views;

import android.content.Context;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.QwertyKeyListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Filter;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import de.blau.android.R;

/**
 * Custom version of AutoCompleteTextView with switchable behaviour of MultiAutoCompleteTextView for supporting OSM
 * lists (aka ; separated values)
 * 
 * Hack: offsets dropdown to the left by the views height and make it wider by the same amount Includes
 * multi-autocomplete logic enabled by setting a tokenizer
 * 
 * @author simon
 * 
 *         Includes code from MultiAutoCompleteTextView.java Copyright (C) 2007 The Android Open Source Project,
 *         Licensed under the Apache License, Version 2.0
 *
 */
public class CustomAutoCompleteTextView extends AppCompatAutoCompleteTextView {

    private static final String DEBUG_TAG = CustomAutoCompleteTextView.class.getSimpleName().substring(0, Math.min(23, CustomAutoCompleteTextView.class.getSimpleName().length()));

    private Tokenizer mTokenizer = null;

    private int parentWidth = -1;

    /**
     * Construct a new instance
     * 
     * @param context Android Context
     */
    public CustomAutoCompleteTextView(Context context) {
        this(context, null);
    }

    /**
     * Construct a new instance
     * 
     * @param context Android Context
     * @param attrs AttributeSet
     */
    public CustomAutoCompleteTextView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.autoCompleteTextViewStyle);
    }

    /**
     * Construct a new instance
     * 
     * @param context Android Context
     * @param attrs an AttributeSet
     * @param defStyleAttr style res id
     */
    public CustomAutoCompleteTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // set a default onClickListener that displays the dropdown
        OnClickListener autocompleteOnClick = v -> {
            if (v.hasFocus()) {
                Log.d(DEBUG_TAG, "onClick");
                ((CustomAutoCompleteTextView) v).showDropDown();
            }
        };
        setOnClickListener(autocompleteOnClick);
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w == 0 && h == 0) {
            return;
        }

        if (parentWidth == -1) {
            // We want normal behaviour
            return;
        }

        // this is not really satisfactory
        int ddw = parentWidth - w;
        setDropDownHorizontalOffset(-ddw);
        int dropDownWidth = getDropDownWidth();
        if (dropDownWidth != ViewGroup.LayoutParams.MATCH_PARENT && dropDownWidth != ViewGroup.LayoutParams.WRAP_CONTENT) {
            setDropDownWidth(ddw);
        }
    }

    /**
     * Set the width of the parent of this View
     * 
     * @param parentWidth the width
     */
    public void setParentWidth(int parentWidth) {
        this.parentWidth = parentWidth;
    }

    /**
     * Sets the Tokenizer that will be used to determine the relevant range of the text where the user is typing.
     * 
     * @param t the Tokenizer instance
     */
    public void setTokenizer(Tokenizer t) {
        mTokenizer = t;
    }

    /**
     * Instead of filtering on the entire contents of the edit box, this subclass method filters on the range from
     * {@link Tokenizer#findTokenStart} to {@link #getSelectionEnd} if the length of that range meets or exceeds
     * {@link #getThreshold}.
     */
    @Override
    protected void performFiltering(CharSequence text, int keyCode) {
        if (mTokenizer == null) {
            super.performFiltering(text, keyCode);
            return;
        }
        if (enoughToFilter()) {
            int end = super.getSelectionEnd();
            int start = mTokenizer.findTokenStart(text, end);
            performFiltering(text, start, end, keyCode);
        } else {
            dismissDropDown();
            Filter f = getFilter();
            if (f != null) {
                f.filter(null);
            }
        }
    }

    /**
     * Instead of filtering whenever the total length of the text exceeds the threshhold, this subclass filters only
     * when the length of the range from {@link Tokenizer#findTokenStart} to {@link #getSelectionEnd} meets or exceeds
     * {@link #getThreshold}.
     */
    @Override
    public boolean enoughToFilter() {
        if (mTokenizer == null) {
            return super.enoughToFilter();
        }
        Editable text = super.getText();
        int end = getSelectionEnd();
        if (end < 0) {
            return false;
        }
        int start = mTokenizer.findTokenStart(text, end);
        return end - start >= getThreshold();
    }

    /**
     * Instead of validating the entire text, this subclass method validates each token of the text individually. Empty
     * tokens are removed.
     */
    @Override
    public void performValidation() {
        if (mTokenizer == null) {
            super.performValidation();
            return;
        }
        Validator v = getValidator();
        if (v == null) {
            return;
        }
        Editable e = getText();
        int i = getText().length();
        while (i > 0) {
            int start = mTokenizer.findTokenStart(e, i);
            int end = mTokenizer.findTokenEnd(e, start);
            CharSequence sub = e.subSequence(start, end);
            if (TextUtils.isEmpty(sub)) {
                e.replace(start, i, "");
            } else if (!v.isValid(sub)) {
                e.replace(start, i, mTokenizer.terminateToken(v.fixText(sub)));
            }
            i = start;
        }
    }

    /**
     * <p>
     * Starts filtering the content of the drop down list. The filtering pattern is the specified range of text from the
     * edit box. Subclasses may override this method to filter with a different pattern, for instance a smaller
     * substring of <code>text</code>.
     * </p>
     */
    private void performFiltering(CharSequence text, int start, int end, int keyCode) {
        Log.d(DEBUG_TAG, "performFiltering 2");
        getFilter().filter(text.subSequence(start, end), this);
    }

    /**
     * <p>
     * Performs the text completion by replacing the range from {@link Tokenizer#findTokenStart} to
     * {@link #getSelectionEnd} by the the result of passing <code>text</code> through {@link Tokenizer#terminateToken}.
     * In addition, the replaced region will be marked as an AutoText substition so that if the user immediately presses
     * DEL, the completion will be undone. Subclasses may override this method to do some different insertion of the
     * content into the edit box.
     * </p>
     *
     * @param text the selected suggestion in the drop down list
     */
    @Override
    protected void replaceText(CharSequence text) {
        if (mTokenizer == null) {
            super.replaceText(text);
        }
    }

    /**
     * setText is final and can't be overridden
     * 
     * @param text the text to set
     */
    public void setOrReplaceText(@NonNull String text) {
        if (mTokenizer == null) {
            super.setText(text);
            return;
        }
        Log.d(DEBUG_TAG, "setOrReplaceText " + text);
        clearComposingText();
        int end = getSelectionEnd();
        int start = mTokenizer.findTokenStart(super.getText(), end);
        Editable editable = super.getText();
        String original = TextUtils.substring(editable, start, end);
        QwertyKeyListener.markAsReplaced(editable, start, end, original);
        editable.replace(start, end, mTokenizer.terminateToken(text));
    }

    public interface Tokenizer {
        /**
         * Returns the start of the token that ends at offset <code>cursor</code> within <code>text</code>.
         * 
         * @param text the whole text
         * @param cursor the offset of the end of the token
         * @return the index of the token start
         */
        int findTokenStart(@NonNull CharSequence text, int cursor);

        /**
         * Returns the end of the token (minus trailing punctuation) that begins at offset <code>cursor</code> within
         * <code>text</code>.
         * 
         * @param text the whole text
         * @param cursor the offset of the start of the token
         * @return the index of the token end
         */
        int findTokenEnd(@NonNull CharSequence text, int cursor);

        /**
         * Returns <code>text</code>, modified, if necessary, to ensure that it ends with a token terminator (for
         * example a space or comma).
         *
         * @param text the text to terminate
         * @return text with a terminator
         */
        @NonNull
        CharSequence terminateToken(@NonNull CharSequence text);
    }

    /**
     * This simple Tokenizer can be used for lists where the items are separated by a single character and one or more
     * spaces.
     */
    public static class SingleCharTokenizer implements Tokenizer {

        static final char DEFAULT   = ';';
        char              separator = DEFAULT;

        /**
         * default constructor
         */
        public SingleCharTokenizer() {
        }

        /**
         * Constructor for potential different separator characters
         * 
         * @param s the separator character
         */
        public SingleCharTokenizer(final char s) {
            separator = s;
        }

        @Override
        public int findTokenStart(CharSequence text, int cursor) {
            int i = cursor;
            while (i > 0 && text.charAt(i - 1) != separator) {
                i--;
            }
            while (i < cursor && text.charAt(i) == ' ') {
                i++;
            }
            return i;
        }

        @Override
        public int findTokenEnd(CharSequence text, int cursor) {
            int i = cursor;
            int len = text.length();
            while (i < len) {
                if (text.charAt(i) == separator) {
                    return i;
                } else {
                    i++;
                }
            }
            return len;
        }

        @Override
        public CharSequence terminateToken(CharSequence text) {
            int i = text.length();
            while (i > 0 && text.charAt(i - 1) == ' ') {
                i--;
            }
            if (i > 0 && text.charAt(i - 1) == separator) {
                return text;
            } else {
                if (text instanceof Spanned) {
                    SpannableString sp = new SpannableString(text + String.valueOf(separator));
                    TextUtils.copySpansFrom((Spanned) text, 0, text.length(), Object.class, sp, 0);
                    return sp;
                } else {
                    return text + String.valueOf(separator);
                }
            }
        }
    }
}
