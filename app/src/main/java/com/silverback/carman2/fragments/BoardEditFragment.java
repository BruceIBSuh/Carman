package com.silverback.carman2.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman2.R;
import com.silverback.carman2.adapters.BoardAttachImageAdapter;

import java.util.ArrayList;
import java.util.List;

public class BoardEditFragment extends Fragment implements
        BoardAttachImageAdapter.OnBoardWriteListener{

    // UIs
    private EditText etPostTitle, etPostBody;
    private RecyclerView recyclerView;
    private BoardAttachImageAdapter imgAdapter;

    // Fields
    private String title, content;
    private List<String> imgUriList;


    public BoardEditFragment() {
        // Required empty public constructor
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null) {
            title = getArguments().getString("postTitle");
            content = getArguments().getString("postContent");
            imgUriList = getArguments().getStringArrayList("uriImgList");
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View localView = inflater.inflate(R.layout.fragment_board_write, container, false);

        etPostTitle = localView.findViewById(R.id.et_board_title);
        etPostBody = localView.findViewById(R.id.et_board_body);
        recyclerView = localView.findViewById(R.id.vg_recycler_images);

        etPostTitle.setText(title);
        etPostBody.setText(content);

        // Create RecyclerView for holding attched pictures which are handled in onActivityResult()
        LinearLayoutManager linearLayout = new LinearLayoutManager(getContext());
        linearLayout.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(linearLayout);

        // Set uri list of attached images to the adapter.
        List<Uri> uriImages = new ArrayList<>();
        for(String uriString : imgUriList) uriImages.add(Uri.parse(uriString));
        imgAdapter = new BoardAttachImageAdapter(uriImages, this);
        recyclerView.setAdapter(imgAdapter);

        return localView;
    }

    @Override
    public void removeGridImage(int position) {

    }
}

