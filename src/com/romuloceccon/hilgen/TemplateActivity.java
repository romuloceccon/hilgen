package com.romuloceccon.hilgen;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class TemplateActivity extends Activity
{
    private Spinner spinnerName;
    private Button buttonNew;
    private Button buttonDelete;
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
        buttonDelete = (Button) findViewById(R.id.button_delete);
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
                createNew();
            }
        });
        
        buttonDelete.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                deleteCurrentWithConfirmation();
            }
        });
        
        buttonSave.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                saveCurrent();
            }
        });
    }

    private void updateControls()
    {
        editName.setText(currentTemplate.getName());
        editOuter.setText(currentTemplate.getOuter());
        editInner.setText(currentTemplate.getInner());
    }
    
    private void createNew()
    {
        currentTemplate = new Template();
        templates.add(currentTemplate);
        
        adapter.notifyDataSetChanged();
        spinnerName.setSelection(templates.indexOf(currentTemplate));
        
        updateControls();
    }
    
    private void deleteCurrentWithConfirmation()
    {
        if (currentTemplate.isDefault())
        {
            showToast(getString(R.string.message_cannot_delete_default));
            return;
        }
        
        DialogInterface.OnClickListener listener =
                new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                if (which == DialogInterface.BUTTON_POSITIVE)
                    deleteCurrent();
            }
        };
        
        AlertDialog.Builder builder =
                new AlertDialog.Builder(TemplateActivity.this);
        builder.setMessage(getString(R.string.message_are_you_sure))
            .setPositiveButton(getString(R.string.yes), listener)
            .setNegativeButton(getString(R.string.no), listener)
            .show();
    }
    
    private void deleteCurrent()
    {
        String name = currentTemplate.getName();
        Template.delete(getApplicationContext(), templates, currentTemplate);
        
        adapter.notifyDataSetChanged();
        if (templates.size() > 0)
        {
            spinnerName.setSelection(0);
            currentTemplate = templates.get(0);
        }
        else
        {
            currentTemplate = new Template();
        }
        
        updateControls();
        
        showToast(getString(R.string.message_deleted_template, name));
    }
    
    private void saveCurrent()
    {
        currentTemplate.setName(editName.getText().toString());
        currentTemplate.setOuter(editOuter.getText().toString());
        currentTemplate.setInner(editInner.getText().toString());
        currentTemplate.save(getApplicationContext(), templates);
        
        adapter.notifyDataSetChanged();
        showToast(getString(R.string.message_saved_template,
                currentTemplate.getName()));
    }
    
    private void showToast(CharSequence msg)
    {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
