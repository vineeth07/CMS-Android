package helper;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import crux.bphc.cms.R;
import set.Content;
import set.Module;

public class ModulesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    CourseDataHandler courseDataHandler;
    private MyFileManager mFileManager;
    private Context context;
    private LayoutInflater inflater;
    private List<Module> modules;
    private ClickListener clickListener;
    private String courseName;
    private int maxDescriptionlines = 3;

    public ModulesAdapter(Context context, MyFileManager fileManager, String courseName) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        modules = new ArrayList<>();
        mFileManager = fileManager;
        this.courseName = courseName;
        courseDataHandler = new CourseDataHandler(context);
    }


    public void setModules(List<Module> modules) {
        this.modules = modules;
        notifyDataSetChanged();
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        return new ViewHolderResource(inflater.inflate(R.layout.row_course_module_resource, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        ((ViewHolderResource) holder).bind(modules.get(position));
    }

    @Override
    public int getItemCount() {
        return modules.size();
    }

    public void setClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    private boolean isNextLabel(int position) {
        position++;
        return modules.size() > position && modules.get(position).getModType() == Module.Type.LABEL;

    }

    class ViewHolderResource extends RecyclerView.ViewHolder {
        HtmlTextView name;
        TextView description;
        ImageView modIcon, more, downloadIcon;
        int downloaded = -1;
        ProgressBar progressBar;
        View iconWrapper, topDivider, bottomDivider;
        View clickWrapper, textWrapper;

        ViewHolderResource(View itemView) {
            super(itemView);

            iconWrapper = itemView.findViewById(R.id.iconWrapper);
            name = itemView.findViewById(R.id.fileName);
            modIcon = itemView.findViewById(R.id.fileIcon);
            more = itemView.findViewById(R.id.more);
            topDivider = itemView.findViewById(R.id.topDivider);
            bottomDivider = itemView.findViewById(R.id.bottomDivider);
            description = itemView.findViewById(R.id.description);
            clickWrapper = itemView.findViewById(R.id.clickWrapper);
            textWrapper = itemView.findViewById(R.id.textWrapper);
            downloadIcon = itemView.findViewById(R.id.downloadButton);
            description.setMovementMethod(LinkMovementMethod.getInstance());
            description.setLinksClickable(true);

            downloadIcon.setOnClickListener(view -> {
                if (clickListener != null) {
                    clickListener.onClick(modules.get(getLayoutPosition()), getLayoutPosition());
                }
                markAsReadandUnread(modules.get(getLayoutPosition()), getLayoutPosition(), false);
            });
            more.setOnClickListener(v -> {
                final Module module = modules.get(getLayoutPosition());
                final int position = getLayoutPosition();
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
                alertDialog.setTitle(module.getName());
                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1);
                if (downloaded == 1) {
                    arrayAdapter.add("View");
                    arrayAdapter.add("Re-Download");
                    arrayAdapter.add("Share");
                    arrayAdapter.add("Mark as Unread");
                } else {
                    arrayAdapter.add("Download");
                    arrayAdapter.add("Mark as Unread");
                }

                alertDialog.setNegativeButton("Cancel", null);
                alertDialog.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (downloaded == 1) {
                            switch (i) {
                                case 0:
                                    if (module.getContents() != null)
                                        for (Content content : module.getContents()) {
                                            mFileManager.openFile(content.getFilename(), courseName);

                                        }
                                    break;
                                case 1:
                                    if (!module.isDownloadable()) {
                                        return;
                                    }

                                    for (Content content : module.getContents()) {
                                        Toast.makeText(context, "Downloading file - " + content.getFilename(), Toast.LENGTH_SHORT).show();
                                        mFileManager.downloadFile(content, module, courseName);
                                    }
                                    break;
                                case 2:
                                    if (module.getContents() != null)
                                        for (Content content : module.getContents()) {
                                            mFileManager.shareFile(content.getFilename(), courseName);
                                        }
                                    break;
                                case 3:
                                    markAsReadandUnread(module, position, true);


                            }
                        } else {
                            switch (i) {
                                case 0:
                                    mFileManager.downloadFile(module.getContents().get(0), module, courseName);
                                    break;
                                case 1:
                                    markAsReadandUnread(module, position, true);
                            }

                        }
                    }
                });
                alertDialog.show();
                markAsReadandUnread(modules.get(getLayoutPosition()), getLayoutPosition(), false);
            });
            progressBar = itemView.findViewById(R.id.progressBar);
        }

        private void markAsReadandUnread(Module module, int position, boolean isNewContent) {
            courseDataHandler.markAsReadandUnread(module.getId(), isNewContent);
            modules.get(position).setNewContent(isNewContent);
            notifyItemChanged(position);
        }

        void bind(Module module) {
            if (module.isNewContent()) {
                itemView.setBackgroundColor(Color.parseColor("#E0F7FA"));
//                name.setTypeface(null, Typeface.BOLD);
//                name.setTextColor(Color.parseColor("#000000"));
            } else {
                itemView.setBackgroundColor(Color.WHITE);
//                name.setTypeface(null, Typeface.NORMAL);
//                name.setTextColor(Color.parseColor("#808080"));
            }

            name.setText(module.getName());
            if (module.getDescription() != null && !module.getDescription().isEmpty()) {
                Spanned htmlDescription = Html.fromHtml(module.getDescription());
                String descriptionWithOutExtraSpace = htmlDescription.toString().trim();
                description.setText(htmlDescription.subSequence(0, descriptionWithOutExtraSpace.length()));
                makeTextViewResizable(description, maxDescriptionlines, "show more", true);
            } else {
                description.setVisibility(View.GONE);
            }
            iconWrapper.setVisibility(View.VISIBLE);
            if (!module.isDownloadable()) {
//                download.setVisibility(View.GONE);
//                name.setTextColor(Color.parseColor("#000000"));
                downloadIcon.setImageResource(R.drawable.eye);
            } else {
//                download.setVisibility(View.VISIBLE);
                List<Content> contents = module.getContents();
                downloaded = 1;
                for (Content content : contents) {
                    if (!mFileManager.searchFile(content.getFilename())) {
                        downloaded = 0;
                        break;
                    }
                }
                if (downloaded == 1) {
//                    download.setImageResource(R.drawable.eye);
//                    name.setTextColor(Color.parseColor("#4CAF50"));
                    downloadIcon.setImageResource(R.drawable.eye);
                } else {
//                    download.setImageResource(R.drawable.content_save);
//                    name.setTextColor(Color.parseColor("#000000"));
                    downloadIcon.setImageResource(R.drawable.download);
                }
            }
            progressBar.setVisibility(View.GONE);
            if (module.getModType() == Module.Type.LABEL) {
                iconWrapper.setVisibility(View.GONE);
            } else {
                int resourceIcon = module.getResourceIcon();
                if (resourceIcon != -1) {
                    modIcon.setImageResource(module.getResourceIcon());
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                    Picasso.with(context).load(module.getModicon()).into(modIcon, new Callback() {
                        @Override
                        public void onSuccess() {
                            progressBar.setVisibility(View.GONE);
                        }

                        @Override
                        public void onError() {

                        }
                    });
                }
            }

            if (isNextLabel(getLayoutPosition()) || getLayoutPosition() == modules.size() - 1) {
                bottomDivider.setVisibility(View.GONE);
            } else {
                bottomDivider.setVisibility(View.VISIBLE);
            }

            if (module.getModType() == Module.Type.LABEL) {
                topDivider.setVisibility(View.VISIBLE);
            } else {
                topDivider.setVisibility(View.GONE);
            }
            more.setVisibility(module.isDownloadable() ? View.VISIBLE : View.GONE);

        }

        public  void makeTextViewResizable(final TextView description, final int maxLine, final String expandText, final boolean viewMore) {

            if (description.getTag() == null) {
                description.setTag(description.getText());
            }
            ViewTreeObserver vto = description.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {


                @Override
                public void onGlobalLayout() {
                    String text;
                    int lineEndIndex;
                    ViewTreeObserver obs = description.getViewTreeObserver();
                    obs.removeOnGlobalLayoutListener(this);
                    if (maxLine == 0) {
                        text = expandText;
                    } else if (maxLine>0 && description.getLineCount() > maxLine) {
                        lineEndIndex = description.getLayout().getLineEnd(maxLine - 1);
                        text = description.getText().subSequence(0, lineEndIndex) + "\n" + expandText;
                    } else if(description.getLineCount() <= maxLine) {
                        text = description.getText().toString();
                    } else {
                        lineEndIndex = description.getLayout().getLineEnd(description.getLayout().getLineCount() - 1);
                        text = description.getText().subSequence(0, lineEndIndex) + "\n" + expandText;
                    }
                    description.setText(text);
                    description.setMovementMethod(LinkMovementMethod.getInstance());
                    description.setText(
                            addClickablePartTextViewResizable(description.getText().toString(), description, expandText,
                                    viewMore), TextView.BufferType.SPANNABLE);
                }
            });

        }

        private  SpannableStringBuilder addClickablePartTextViewResizable(
                final String spannedString, final TextView description, final String spanableText, final boolean viewMore) {

            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(spannedString);

            if (spannedString.contains(spanableText)) {
                spannableStringBuilder.setSpan(new ClickableSpan() {

                    @Override
                    public void onClick(View widget) {

                        description.setLayoutParams(description.getLayoutParams());
                        description.setText(description.getTag().toString(), TextView.BufferType.SPANNABLE);
                        description.invalidate();
                        if (viewMore) {
                            makeTextViewResizable(description, -1, "show less", false);
                        } else {
                            makeTextViewResizable(description, maxDescriptionlines, "show more", true);
                        }

                    }

                    @Override
                    public void updateDrawState(TextPaint textpaint) {
                        super.updateDrawState(textpaint);
                        textpaint.setColor(Color.BLACK);
                        textpaint.setUnderlineText(false);
                        textpaint.setFakeBoldText(true);
                    }
                }, spannedString.indexOf(spanableText), spannedString.indexOf(spanableText) + spanableText.length(), 0);

            }
            description.setHighlightColor(Color.TRANSPARENT);
            return spannableStringBuilder;

        }
    }

}