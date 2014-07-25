package com.romuloceccon.hilgen;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class TemplateActivity extends Activity
{
    private Spinner spinnerName;
    private Button buttonNew;
    private EditText editName;
    private EditText editOuter;
    private EditText editInner;
    private Button buttonSave;

    private Template currentTemplate = new Template();
    private List<Template> templates = new ArrayList<Template>();
    private ArrayAdapter<Template> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_template);
        
        Template.getAll(getApplicationContext(), templates);
        
        spinnerName = (Spinner) findViewById(R.id.spinner_name);
        buttonNew = (Button) findViewById(R.id.button_new);
        editName = (EditText) findViewById(R.id.edit_name);
        editOuter = (EditText) findViewById(R.id.edit_outer);
        editInner = (EditText) findViewById(R.id.edit_inner);
        buttonSave = (Button) findViewById(R.id.button_save);
        
        adapter = new ArrayAdapter<Template>(this,
                android.R.layout.simple_spinner_item, templates);
        spinnerName.setAdapter(adapter);
        
        spinnerName.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v,
                    int position, long id)
            {
                currentTemplate = templates.get(position);
                updateControls();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0)
            {
                currentTemplate = new Template();
                updateControls();
            }
        });
        
        buttonNew.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                currentTemplate = new Template();
                updateControls();
            }
        });
        
        buttonSave.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                currentTemplate.setName(editName.getText().toString());
                currentTemplate.setOuter(editOuter.getText().toString());
                currentTemplate.setInner(editInner.getText().toString());
                currentTemplate.save(getApplicationContext(), templates);
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void updateControls()
    {
        editName.setText(currentTemplate.getName());
        editOuter.setText(currentTemplate.getOuter());
        editInner.setText(currentTemplate.getInner());
    }
}
