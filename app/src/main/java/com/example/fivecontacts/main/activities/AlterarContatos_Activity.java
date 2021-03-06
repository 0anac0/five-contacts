package com.example.fivecontacts.main.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.example.fivecontacts.R;
import com.example.fivecontacts.main.model.Contato;
import com.example.fivecontacts.main.model.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class AlterarContatos_Activity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    EditText edtNome, edtNomeRemover;
    ListView lv, lr;
    BottomNavigationView bnv;
    User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alterar_contatos);
        edtNome = findViewById(R.id.edtBusca);
        edtNomeRemover = findViewById(R.id.edtBuscaRemover);
        bnv = findViewById(R.id.bnv);
        bnv.setOnNavigationItemSelectedListener(this);
        bnv.setSelectedItemId(R.id.anvMudar);

        //Dados da Intent Anterior
        Intent quemChamou=this.getIntent();
        if (quemChamou!=null) {
            Bundle params = quemChamou.getExtras();
            if (params!=null) {
                //Recuperando o Usuario
                user = (User) params.getSerializable("usuario");
                setTitle("Alterar Contatos de Emerg??ncia");
            }
        }
        lv = findViewById(R.id.cellphoneContacts);
        lr = findViewById(R.id.appContacts);

    }

    public void salvarContato (Contato w){
        SharedPreferences salvaContatos =
                getSharedPreferences("contatos",Activity.MODE_PRIVATE);

        int num = salvaContatos.getInt("numContatos", 0); //checando quantos contatos j?? tem
        SharedPreferences.Editor editor = salvaContatos.edit();
        try {
            ByteArrayOutputStream dt = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(dt);
            dt = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(dt);
            oos.writeObject(w);
            String contatoSerializado= dt.toString(StandardCharsets.ISO_8859_1.name());
            editor.putString("contato"+(num+1), contatoSerializado);
            editor.putInt("numContatos",num+1);
        }catch(Exception e){
            e.printStackTrace();
        }
        editor.commit();
        user.getContatos().add(w);
    }

    public void removeContact(Contato contato) {
        SharedPreferences savedContacts =
                getSharedPreferences("contatos",Activity.MODE_PRIVATE);

        SharedPreferences.Editor editor = savedContacts.edit();
        editor.clear();
        user.removeContact(contato);
        ArrayList<Contato> contatos = user.getContatos();

        int num = 0;
        try {
            for (int i = 0; i < contatos.size(); i++){
                ByteArrayOutputStream dt = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(dt);
                oos.writeObject(contatos.get(i));
                String contatoSerializado= dt.toString(StandardCharsets.ISO_8859_1.name());
                editor.putString("contato"+(num+1), contatoSerializado);
                editor.putInt("numContatos",num+1);

            }
        }catch(Exception e){
            e.printStackTrace();
        }
        editor.apply();

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    public void onClickBuscar(View v){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, 3333);
            return;
        }

        ContentResolver cr = getContentResolver();
        String consulta = ContactsContract.Contacts.DISPLAY_NAME + " LIKE ?";
        String [] argumentosConsulta= {"%"+edtNome.getText()+"%"};
        Cursor cursor= cr.query(ContactsContract.Contacts.CONTENT_URI, null,
                consulta,argumentosConsulta, null);
        final String[] nomesContatos = new String[cursor.getCount()];
        final String[] telefonesContatos = new String[cursor.getCount()];

        int i=0;
        while (cursor.moveToNext()) {
            int indiceNome = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME);
            String contatoNome = cursor.getString(indiceNome);
            nomesContatos[i]= contatoNome;
            int indiceContatoID = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID);
            String contactID = cursor.getString(indiceContatoID);
            String consultaPhone = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactID;
            Cursor phones = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null, consultaPhone, null, null);

            while (phones.moveToNext()) {
                String number = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                telefonesContatos[i]=number; //Salvando s?? ??ltimo telefone
            }
            i++;
        }

        if (nomesContatos !=null) {
            for(int j=0; j<=nomesContatos.length; j++) {
                ArrayAdapter<String> adaptador;
                adaptador = new ArrayAdapter<String>(this, R.layout.list_view_layout, nomesContatos);
                lv.setAdapter(adaptador);
                lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        Contato c= new Contato();
                        c.setNome(nomesContatos[i]);
                        c.setNumero("tel:"+telefonesContatos[i]);
                        if (duplicatedContact(c, user)){
                            Toast.makeText(getApplicationContext(), "Contato duplicado!", Toast.LENGTH_LONG)
                                    .show();
                        } else {
                            salvarContato(c);
                            Intent intent = new Intent(getApplicationContext(), ListaDeContatos_Activity.class);
                            intent.putExtra("usuario", user);
                            startActivity(intent);
                            finish();
                        }

                    }
                });
            }
        }
    }



    public void onClickBuscarRemover(View v){
        final ArrayList<Contato> contatos = user.getContatosLike(edtNomeRemover.getText().toString());
        int totalContatos = contatos.size();

        final String[] nomesContatos = new String[totalContatos];
        for (int i = 0; i< totalContatos; i++) {
            nomesContatos[i]=contatos.get(i).getNome();
        }

        if (totalContatos != 0) {
            for(int i=0; i<=totalContatos; i++) {
                ArrayAdapter<String> adaptador;
                adaptador = new ArrayAdapter<String>(this, R.layout.list_view_layout, nomesContatos);
                lr.setAdapter(adaptador);
                lr.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        Contato c= new Contato();
                        c.setNome(nomesContatos[i]);
                        c.setNumero(contatos.get(i).getNumero());
                        removeContact(c);
                        Toast.makeText(getApplicationContext(), "Contato removido!", Toast.LENGTH_LONG)
                                .show();
                        Intent intent = new Intent(getApplicationContext(), ListaDeContatos_Activity.class);
                        intent.putExtra("usuario", user);
                        startActivity(intent);
                        finish();

                    }
                });
            }
        }
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Checagem de o Item selecionado ?? o do perfil
        if (item.getItemId() == R.id.anvPerfil) {
            //Abertura da Tela MudarDadosUsario
            Intent intent = new Intent(this, PerfilUsuario_Activity.class);
            intent.putExtra("usuario", user);
            startActivity(intent);

        }
        // Checagem de o Item selecionado ?? o do perfil
        if (item.getItemId() == R.id.anvLigar) {
            //Abertura da Tela Mudar COntatos
            Intent intent = new Intent(this, ListaDeContatos_Activity.class);
            intent.putExtra("usuario", user);
            startActivity(intent);

        }
        return true;
    }


    public boolean duplicatedContact (Contato contato, User user) {
        final ArrayList<Contato> contatos = user.getContatos();
        boolean duplicated = false;
        loop:
        for (int i = 0; i<contatos.size(); i++) {
            if (contato.sameContact(contatos.get(i))) {
                duplicated = true;
                break loop;
            }
        }
        return duplicated;
    };
}