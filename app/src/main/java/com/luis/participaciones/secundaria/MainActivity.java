package com.luis.participaciones.secundaria;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.Settings;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    static final int NAVY = Color.rgb(24, 43, 94);
    static final int PURPLE = Color.rgb(102, 92, 231);
    static final int BG = Color.rgb(247, 248, 252);
    static final int CARD = Color.WHITE;
    static final int GREEN = Color.rgb(52, 181, 112);
    static final int RED = Color.rgb(232, 79, 96);
    static final int AMBER = Color.rgb(224, 166, 46);
    static final int MUTED = Color.rgb(105, 116, 139);
    static final int BORDER = Color.rgb(225, 229, 239);

    DB db;
    LinearLayout root;
    long currentGroupId = -1;
    long currentStudentId = -1;
    String currentGroupName = "";
    int screen = 0; // 0 home, 1 session, 2 history, 3 groups, 4 settings, 5 round detail, 6 student history, 7 stats
    int studentReturnScreen = 5;
    long detailRoundId = -1;
    long detailStudentId = -1;
    long statsGroupId = -1;
    String statsFrom = "", statsTo = "";
    final Random random = new Random();
    static final int REQ_BACKUP = 501;
    static final int REQ_RESTORE = 502;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        db = new DB(this);
        db.getWritableDatabase();
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        if (Build.VERSION.SDK_INT >= 23) getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        showHome();
    }

    int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }

    GradientDrawable bg(int color, int radius) {
        GradientDrawable g = new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(radius)); return g;
    }
    GradientDrawable bgStroke(int color, int radius, int strokeColor) {
        GradientDrawable g = bg(color, radius); g.setStroke(dp(1), strokeColor); return g;
    }
    TextView tv(String text, int size, int color, boolean bold) {
        TextView v = new TextView(this); v.setText(text); v.setTextSize(size); v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD); v.setGravity(Gravity.CENTER_VERTICAL); return v;
    }
    TextView label(String text) { TextView v=tv(text,14,MUTED,false); v.setPadding(0,dp(2),0,dp(2)); return v; }
    Button button(String text, int color) {
        Button b = new Button(this); b.setText(text); b.setTextColor(Color.WHITE); b.setTextSize(15); b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD); b.setBackground(bg(color,14)); b.setMinHeight(dp(48)); return b;
    }
    LinearLayout vertical() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    LinearLayout horizontal() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    LinearLayout.LayoutParams lp(int w,int h,float weight){ return new LinearLayout.LayoutParams(w,h,weight); }
    void margin(View v,int l,int t,int r,int b){ LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)v.getLayoutParams(); p.setMargins(dp(l),dp(t),dp(r),dp(b)); v.setLayoutParams(p); }

    void base(String title, boolean back) {
        root = vertical(); root.setBackgroundColor(BG); root.setPadding(dp(22),dp(16),dp(22),dp(22));
        ScrollView sv = new ScrollView(this); sv.setFillViewport(true); sv.addView(root); setContentView(sv);
        LinearLayout head = horizontal();
        if (back) {
            Button b = button("‹", NAVY); b.setTextSize(26); b.setPadding(0,0,0,dp(3)); b.setLayoutParams(lp(dp(52),dp(48),0));
            b.setOnClickListener(v->goBack()); head.addView(b);
        }
        TextView t = tv(title,22,NAVY,true); t.setGravity(Gravity.CENTER); head.addView(t,lp(0,dp(52),1));
        if (back) { Space s=new Space(this); head.addView(s,lp(dp(52),dp(48),0)); }
        root.addView(head,lp(-1,dp(56),0));
    }

    void showHome() {
        screen=0; currentStudentId=-1; base("Participaciones Secundaria", false);
        TextView school = tv("COLEGIO VALLADOLID LA PAZ CENTRO",13,MUTED,true); school.setGravity(Gravity.CENTER); root.addView(school);
        TextView sub = tv("Elige un grupo para iniciar o continuar una ronda",15,MUTED,false); sub.setGravity(Gravity.CENTER); sub.setPadding(0,dp(6),0,dp(18)); root.addView(sub);

        ArrayList<Group> gs = db.groups();
        LinearLayout grid = vertical(); root.addView(grid);
        for (int i=0;i<gs.size();i+=2) {
            LinearLayout row=horizontal(); row.setGravity(Gravity.CENTER); grid.addView(row,lp(-1,-2,0));
            for (int j=0;j<2;j++) {
                if (i+j<gs.size()) {
                    Group g=gs.get(i+j); LinearLayout c=groupCard(g, (i+j)%4); row.addView(c,lp(0,dp(128),1)); margin(c,6,6,6,6);
                } else { Space s=new Space(this); row.addView(s,lp(0,dp(128),1)); }
            }
        }
        LinearLayout actions=horizontal(); actions.setPadding(0,dp(18),0,0); root.addView(actions);
        Button hist=button("Historial",PURPLE); hist.setOnClickListener(v->showHistory()); actions.addView(hist,lp(0,dp(52),1)); margin(hist,4,0,4,0);
        Button stat=button("Estadísticas",GREEN); stat.setOnClickListener(v->showStats()); actions.addView(stat,lp(0,dp(52),1)); margin(stat,4,0,4,0);
        Button adm=button("Grupos",NAVY); adm.setOnClickListener(v->showGroups()); actions.addView(adm,lp(0,dp(52),1)); margin(adm,4,0,4,0);
        Button set=button("Ajustes",Color.rgb(83,96,121)); set.setOnClickListener(v->showSettings()); actions.addView(set,lp(0,dp(52),1)); margin(set,4,0,4,0);
        TextView foot=label("© 2026 Profesor Luis Ángel · Todos los derechos reservados"); foot.setGravity(Gravity.CENTER); foot.setPadding(0,dp(24),0,0); root.addView(foot);
    }

    LinearLayout groupCard(Group g,int idx){
        int[] cols={Color.rgb(225,240,255),Color.rgb(222,248,231),Color.rgb(255,240,202),Color.rgb(255,225,231)};
        LinearLayout c=vertical(); c.setPadding(dp(18),dp(15),dp(18),dp(15)); c.setGravity(Gravity.CENTER_VERTICAL); c.setBackground(bgStroke(cols[idx],18,BORDER));
        TextView n=tv(g.name,28,NAVY,true); c.addView(n);
        int total=db.studentCount(g.id); int pending=db.pendingCount(g.id);
        c.addView(label(total+" alumnos · "+pending+" pendientes"));
        TextView go=tv("Abrir ronda  ›",14,PURPLE,true); go.setPadding(0,dp(9),0,0); c.addView(go);
        c.setOnClickListener(v->{currentGroupId=g.id; currentGroupName=g.name; db.ensureRound(g.id); currentStudentId=-1; showSession();}); return c;
    }

    void showSession(){
        screen=1; base(currentGroupName, true); long round=db.ensureRound(currentGroupId);
        TextView roundTitle=tv(db.roundLabel(round),14,MUTED,true); roundTitle.setGravity(Gravity.CENTER); root.addView(roundTitle);
        LinearLayout top=horizontal(); top.setPadding(0,dp(8),0,dp(10)); root.addView(top);
        int consumed=db.consumedCount(round), total=db.studentCount(currentGroupId), pending=db.pendingCount(currentGroupId);
        TextView stats=tv(consumed+" seleccionados   ·   "+pending+" pendientes de "+total,15,NAVY,true); top.addView(stats,lp(0,dp(46),1));
        Button list=button("Ver lista",PURPLE); list.setLayoutParams(lp(dp(120),dp(46),0)); list.setOnClickListener(v->showPendingDialog()); top.addView(list);

        LinearLayout card=vertical(); card.setPadding(dp(24),dp(22),dp(24),dp(22)); card.setBackground(bgStroke(Color.rgb(239,245,255),20,Color.rgb(210,222,247))); root.addView(card,lp(-1,-2,0));
        TextView small=tv(currentStudentId>0?"¡Es tu turno!":"Listo para elegir alumno",18,NAVY,true); small.setGravity(Gravity.CENTER); card.addView(small);
        Student s=currentStudentId>0?db.student(currentStudentId):null;
        TextView name=tv(s==null?"Presiona “Elegir alumno”":s.name,30,NAVY,true); name.setGravity(Gravity.CENTER); name.setPadding(0,dp(16),0,dp(6)); card.addView(name);
        TextView mat=tv(s==null?"Selección aleatoria sin repetir":"Matrícula: "+safe(s.matricula),15,MUTED,false); mat.setGravity(Gravity.CENTER); card.addView(mat);

        LinearLayout statuses=horizontal(); statuses.setPadding(0,dp(16),0,dp(8)); root.addView(statuses);
        Button yes=button("✓ Participó",GREEN), no=button("✕ No respondió",RED), absent=button("— Ausente",AMBER);
        yes.setEnabled(s!=null); no.setEnabled(s!=null); absent.setEnabled(s!=null);
        statuses.addView(yes,lp(0,dp(68),1)); margin(yes,4,0,4,0); statuses.addView(no,lp(0,dp(68),1)); margin(no,4,0,4,0); statuses.addView(absent,lp(0,dp(68),1)); margin(absent,4,0,4,0);
        yes.setOnClickListener(v->saveResult("P")); no.setOnClickListener(v->saveResult("N")); absent.setOnClickListener(v->saveResult("A"));
        Button next=button(s==null?"Elegir alumno al azar":"Siguiente alumno  →",PURPLE); next.setOnClickListener(v->pickStudent()); root.addView(next,lp(-1,dp(58),0)); margin(next,0,8,0,0);

        LinearLayout res=horizontal(); res.setPadding(0,dp(18),0,0); root.addView(res);
        addMiniStat(res,"Participaron",db.resultCount(round,"P"),GREEN); addMiniStat(res,"No respondieron",db.resultCount(round,"N"),RED); addMiniStat(res,"Ausencias",db.resultCount(round,"A"),AMBER);
        Button restart=button("Reiniciar ronda",Color.rgb(88,99,128)); restart.setOnClickListener(v->confirmRestart()); root.addView(restart,lp(-1,dp(50),0)); margin(restart,0,18,0,0);
        TextView note=label("Reiniciar una ronda NO borra sus registros. Puedes reiniciarla cuando quieras, aunque no hayan pasado todos."); note.setPadding(dp(8),dp(12),dp(8),0); root.addView(note);
    }

    void addMiniStat(LinearLayout row,String title,int value,int color){
        LinearLayout c=vertical(); c.setGravity(Gravity.CENTER); c.setPadding(dp(6),dp(12),dp(6),dp(12)); c.setBackground(bgStroke(Color.WHITE,14,BORDER));
        TextView v=tv(String.valueOf(value),24,color,true); v.setGravity(Gravity.CENTER); c.addView(v); TextView t=tv(title,12,MUTED,true); t.setGravity(Gravity.CENTER); c.addView(t); row.addView(c,lp(0,dp(78),1)); margin(c,4,0,4,0);
    }

    void pickStudent(){
        ArrayList<Student> p=db.pendingStudents(currentGroupId); if(p.isEmpty()){ new AlertDialog.Builder(this).setTitle("Ronda completa").setMessage("Ya no quedan alumnos pendientes. Puedes consultar el historial o iniciar una nueva ronda.").setPositiveButton("Nueva ronda",(d,w)->{db.restartRound(currentGroupId);currentStudentId=-1;showSession();}).setNegativeButton("Cerrar",null).show(); return; }
        if (p.size()>1 && currentStudentId>0) p.removeIf(x->x.id==currentStudentId);
        if(p.isEmpty()) p=db.pendingStudents(currentGroupId);
        currentStudentId=p.get(random.nextInt(p.size())).id; showSession();
    }

    void saveResult(String r){
        if(currentStudentId<=0)return; long round=db.ensureRound(currentGroupId); db.addParticipation(round,currentGroupId,currentStudentId,r);
        Student s=db.student(currentStudentId); String text = r.equals("P")?"Participó":r.equals("N")?"No respondió":"Ausente (seguirá pendiente)";
        Toast.makeText(this,s.name+": "+text,Toast.LENGTH_SHORT).show(); currentStudentId=-1; showSession();
    }

    void showPendingDialog(){
        long round=db.ensureRound(currentGroupId); LinearLayout box=vertical(); box.setPadding(dp(18),dp(8),dp(18),dp(8));
        TextView h=tv("Pendientes ("+db.pendingCount(currentGroupId)+")",18,NAVY,true); box.addView(h);
        for(Student s:db.pendingStudents(currentGroupId)){ TextView x=tv(s.name+"   ·   "+safe(s.matricula),14,NAVY,false); x.setPadding(dp(8),dp(10),dp(8),dp(10)); box.addView(x); x.setOnClickListener(v->{currentStudentId=s.id;}); }
        ScrollView sv=new ScrollView(this); sv.addView(box); new AlertDialog.Builder(this).setTitle(currentGroupName+" · "+db.roundLabel(round)).setView(sv).setPositiveButton("Cerrar",null).show();
    }

    void confirmRestart(){
        long r=db.ensureRound(currentGroupId); new AlertDialog.Builder(this).setTitle("¿Reiniciar ronda?").setMessage("Se iniciará una nueva ronda para "+currentGroupName+".\n\nLa ronda actual y todos sus registros se conservarán en el historial. Los alumnos volverán a quedar disponibles para selección.").setNegativeButton("Cancelar",null).setPositiveButton("Reiniciar",(d,w)->{db.restartRound(currentGroupId);currentStudentId=-1;showSession();}).show();
    }

    void showHistory(){
        screen=2; base("Historial general", true); ArrayList<RoundRow> rs=db.roundsAll();
        if(rs.isEmpty()){root.addView(label("Todavía no hay rondas registradas."));return;}
        for(RoundRow r:rs){ LinearLayout c=vertical(); c.setPadding(dp(16),dp(12),dp(16),dp(12)); c.setBackground(bgStroke(CARD,14,BORDER)); root.addView(c,lp(-1,-2,0)); margin(c,0,5,0,5);
            TextView t=tv(r.groupName+" · "+r.label,17,NAVY,true); c.addView(t); c.addView(label(r.total+" registros · "+r.p+" participaron · "+r.n+" no respondieron · "+r.a+" ausencias"));
            c.setOnClickListener(v->{detailRoundId=r.id; currentGroupId=r.groupId; currentGroupName=r.groupName; showRoundDetail();});
        }
    }

    void showRoundDetail(){
        screen=5; base(currentGroupName+" · Detalle de ronda",true); RoundRow r=db.round(detailRoundId); if(r==null){showHistory();return;}
        TextView title=tv(r.label,17,MUTED,true); title.setGravity(Gravity.CENTER); root.addView(title);
        LinearLayout row=horizontal(); row.setPadding(0,dp(12),0,dp(12)); root.addView(row); addMiniStat(row,"Participaron",r.p,GREEN);addMiniStat(row,"No respondieron",r.n,RED);addMiniStat(row,"Ausencias",r.a,AMBER);
        for(Participation p:db.participations(detailRoundId)){ LinearLayout c=horizontal(); c.setPadding(dp(14),dp(10),dp(14),dp(10)); c.setBackground(bgStroke(CARD,12,BORDER)); root.addView(c,lp(-1,-2,0)); margin(c,0,3,0,3);
            TextView n=tv(p.name+"\n"+safe(p.matricula),14,NAVY,true); c.addView(n,lp(0,-2,1)); TextView st=tv(resultText(p.result),14,resultColor(p.result),true); st.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL); c.addView(st,lp(dp(150),-1,0));
            c.setOnClickListener(v->{detailStudentId=p.studentId; studentReturnScreen=5; showStudentHistory();});
        }
    }

    void showStudentHistory(){
        screen=6; Student s=db.student(detailStudentId); if(s==null){showRoundDetail();return;} base(s.name,true); root.addView(label("Matrícula: "+safe(s.matricula)));
        int p=db.studentResultCount(s.id,"P"), n=db.studentResultCount(s.id,"N"), a=db.studentResultCount(s.id,"A"); LinearLayout row=horizontal(); row.setPadding(0,dp(14),0,dp(14));root.addView(row);addMiniStat(row,"Participaciones",p,GREEN);addMiniStat(row,"No respondió",n,RED);addMiniStat(row,"Ausencias",a,AMBER);
        for(Participation x:db.studentHistory(s.id)){ LinearLayout c=horizontal();c.setPadding(dp(14),dp(10),dp(14),dp(10));c.setBackground(bgStroke(CARD,12,BORDER));root.addView(c,lp(-1,-2,0));margin(c,0,3,0,3); TextView d=tv(x.time,14,NAVY,false);c.addView(d,lp(0,-2,1));TextView st=tv(resultText(x.result),14,resultColor(x.result),true);c.addView(st); }
    }

    String resultText(String r){return r.equals("P")?"Participó":r.equals("N")?"No respondió":"Ausente";} int resultColor(String r){return r.equals("P")?GREEN:r.equals("N")?RED:AMBER;}

    void showStats(){
        screen=7; base("Estadísticas por periodo",true);
        ArrayList<Group> gs=db.groups(); if(gs.isEmpty()){root.addView(label("No hay grupos."));return;}
        if(statsGroupId<0) statsGroupId=gs.get(0).id;
        Calendar cal=Calendar.getInstance();
        if(statsTo.isEmpty()) statsTo=new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).format(cal.getTime());
        if(statsFrom.isEmpty()){cal.set(Calendar.DAY_OF_MONTH,1);statsFrom=new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).format(cal.getTime());}
        LinearLayout filters=horizontal(); root.addView(filters);
        Spinner sp=new Spinner(this); ArrayList<String> names=new ArrayList<>(); int sel=0; for(int i=0;i<gs.size();i++){names.add(gs.get(i).name);if(gs.get(i).id==statsGroupId)sel=i;} sp.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,names));sp.setSelection(sel);filters.addView(sp,lp(0,dp(50),1));
        Button from=button("Desde: "+statsFrom,NAVY); filters.addView(from,lp(0,dp(50),1));margin(from,4,0,4,0); Button to=button("Hasta: "+statsTo,NAVY);filters.addView(to,lp(0,dp(50),1));
        final int[] selected={sel}; sp.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){selected[0]=pos;statsGroupId=gs.get(pos).id;}public void onNothingSelected(android.widget.AdapterView<?> p){}});
        from.setOnClickListener(v->pickDate(statsFrom,d->{statsFrom=d;showStats();})); to.setOnClickListener(v->pickDate(statsTo,d->{statsTo=d;showStats();}));
        TextView range=label("Cuenta todos los registros guardados, aunque pertenezcan a rondas diferentes.");range.setPadding(0,dp(12),0,dp(10));root.addView(range);
        ArrayList<StatRow> rows=db.statsByPeriod(statsGroupId,statsFrom,statsTo);
        int tp=0,tn=0,ta=0;for(StatRow r:rows){tp+=r.p;tn+=r.n;ta+=r.a;} LinearLayout total=horizontal();root.addView(total);addMiniStat(total,"Participaciones",tp,GREEN);addMiniStat(total,"No respondió",tn,RED);addMiniStat(total,"Ausencias",ta,AMBER);
        TextView h=tv("Participaciones por alumno",17,NAVY,true);h.setPadding(0,dp(18),0,dp(8));root.addView(h);
        for(StatRow r:rows){LinearLayout c=horizontal();c.setPadding(dp(14),dp(10),dp(14),dp(10));c.setBackground(bgStroke(CARD,12,BORDER));root.addView(c,lp(-1,-2,0));margin(c,0,3,0,3);TextView n=tv(r.name+"
"+safe(r.matricula),14,NAVY,true);c.addView(n,lp(0,-2,1));TextView vals=tv("✓ "+r.p+"   ✕ "+r.n+"   — "+r.a,14,NAVY,true);vals.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);c.addView(vals);c.setOnClickListener(v->{detailStudentId=r.studentId;studentReturnScreen=7;showStudentHistory();});}
    }
    interface DateChosen{void set(String d);}
    void pickDate(String current,DateChosen cb){try{String[] p=current.split("-");int y=Integer.parseInt(p[0]),m=Integer.parseInt(p[1])-1,d=Integer.parseInt(p[2]);DatePickerDialog dlg=new DatePickerDialog(this,(v,yy,mm,dd)->cb.set(String.format(Locale.getDefault(),"%04d-%02d-%02d",yy,mm+1,dd)),y,m,d);dlg.show();}catch(Exception e){}}

    void showGroups(){
        screen=3; base("Administrar grupos",true); Button add=button("＋ Agregar grupo",PURPLE);add.setOnClickListener(v->addGroupDialog());root.addView(add,lp(-1,dp(50),0));margin(add,0,4,0,14);
        for(Group g:db.groups()){ LinearLayout c=horizontal();c.setPadding(dp(14),dp(10),dp(14),dp(10));c.setBackground(bgStroke(CARD,14,BORDER));root.addView(c,lp(-1,-2,0));margin(c,0,4,0,4); TextView n=tv(g.name+"\n"+db.studentCount(g.id)+" alumnos",16,NAVY,true);c.addView(n,lp(0,-2,1)); Button alum=button("Alumnos",PURPLE);alum.setOnClickListener(v->studentsDialog(g));c.addView(alum,lp(dp(110),dp(46),0));margin(alum,4,0,4,0); Button del=button("Eliminar",RED);del.setOnClickListener(v->deleteGroupConfirm(g));c.addView(del,lp(dp(100),dp(46),0)); }
    }

    void addGroupDialog(){ EditText e=new EditText(this);e.setHint("Ej. 3BS");e.setTextSize(18); new AlertDialog.Builder(this).setTitle("Nuevo grupo").setView(e).setNegativeButton("Cancelar",null).setPositiveButton("Agregar",(d,w)->{String n=e.getText().toString().trim().toUpperCase();if(!n.isEmpty()){db.addGroup(n);showGroups();}}).show(); }

    void studentsDialog(Group g){
        LinearLayout box=vertical();box.setPadding(dp(14),0,dp(14),dp(8)); TextView info=label("Formato para importar: MATRICULA | NOMBRE (uno por línea)");box.addView(info); EditText text=new EditText(this);text.setHint("1533 | AGUIRRE QUIÑONEZ MATIAS EDUARDO");text.setMinLines(4);text.setGravity(Gravity.TOP);box.addView(text,lp(-1,dp(150),0));
        Button imp=button("Importar / agregar alumnos",PURPLE);box.addView(imp,lp(-1,dp(48),0));margin(imp,0,8,0,8); TextView list=tv(db.studentsText(g.id),13,NAVY,false);box.addView(list);
        ScrollView sv=new ScrollView(this);sv.addView(box); AlertDialog dlg=new AlertDialog.Builder(this).setTitle(g.name+" · Alumnos").setView(sv).setPositiveButton("Cerrar",null).create(); imp.setOnClickListener(v->{int c=db.importStudents(g.id,text.getText().toString());Toast.makeText(this,c+" alumnos agregados/actualizados",Toast.LENGTH_SHORT).show();dlg.dismiss();showGroups();}); dlg.show();
    }

    void deleteGroupConfirm(Group g){ new AlertDialog.Builder(this).setTitle("Eliminar "+g.name+"?").setMessage("Esta acción eliminará el grupo, sus alumnos, rondas y registros de participación.\n\nSe recomienda crear un respaldo desde Ajustes antes de continuar.").setNegativeButton("Cancelar",null).setPositiveButton("Eliminar",(d,w)->{db.deleteGroup(g.id);showGroups();}).show(); }

    void showSettings(){
        screen=4;base("Ajustes",true); addSetting("Respaldo de datos","Guardar una copia completa de grupos, alumnos, rondas y registros",()->backup()); addSetting("Restaurar datos","Reemplazar la base local por un respaldo anterior",()->restore()); addSetting("Historial general","Consultar todas las rondas guardadas",()->showHistory()); addSetting("Estadísticas por periodo","Contar participaciones, respuestas y ausencias entre dos fechas",()->showStats()); addSetting("Administrar grupos","Agregar grupos e importar alumnos",()->showGroups()); addSetting("Acerca de","Participaciones Secundaria v1.0\nCOLEGIO VALLADOLID LA PAZ CENTRO",()->new AlertDialog.Builder(this).setTitle("Participaciones").setMessage("Versión 1.0\n\n© 2026 Profesor Luis Ángel").setPositiveButton("Cerrar",null).show());
    }
    interface Action{void run();}
    void addSetting(String title,String sub,Action a){LinearLayout c=vertical();c.setPadding(dp(16),dp(12),dp(16),dp(12));c.setBackground(bgStroke(CARD,14,BORDER));root.addView(c,lp(-1,-2,0));margin(c,0,4,0,4);c.addView(tv(title,16,NAVY,true));c.addView(label(sub));c.setOnClickListener(v->a.run());}

    void backup(){ Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/octet-stream");i.putExtra(Intent.EXTRA_TITLE,"Participaciones_respaldo_"+new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).format(new Date())+".db");startActivityForResult(i,REQ_BACKUP); }
    void restore(){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");startActivityForResult(i,REQ_RESTORE); }
    @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(res!=RESULT_OK||data==null||data.getData()==null)return;try{ if(req==REQ_BACKUP){db.close();copyFile(getDatabasePath(DB.NAME),data.getData(),true);db=new DB(this);Toast.makeText(this,"Respaldo guardado",Toast.LENGTH_LONG).show();} else if(req==REQ_RESTORE){new AlertDialog.Builder(this).setTitle("Restaurar respaldo").setMessage("Esto reemplazará los datos actuales de la app.").setNegativeButton("Cancelar",null).setPositiveButton("Restaurar",(d,w)->{try{db.close();copyFile(getDatabasePath(DB.NAME),data.getData(),false);db=new DB(this);db.getWritableDatabase();Toast.makeText(this,"Datos restaurados",Toast.LENGTH_LONG).show();showHome();}catch(Exception e){Toast.makeText(this,"No se pudo restaurar: "+e.getMessage(),Toast.LENGTH_LONG).show();}}).show();}}catch(Exception e){db=new DB(this);Toast.makeText(this,"Error: "+e.getMessage(),Toast.LENGTH_LONG).show();}}
    void copyFile(File dbFile,Uri uri,boolean fromDb)throws Exception{ if(fromDb){try(InputStream in=new FileInputStream(dbFile);OutputStream out=getContentResolver().openOutputStream(uri)){copy(in,out);}}else{File tmp=new File(getCacheDir(),"restore.db");try(InputStream in=getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(tmp)){copy(in,out);} try(InputStream in=new FileInputStream(tmp);OutputStream out=new FileOutputStream(dbFile)){copy(in,out);} tmp.delete();}}
    void copy(InputStream in,OutputStream out)throws Exception{byte[] b=new byte[8192];int n;while((n=in.read(b))>0)out.write(b,0,n);out.flush();}

    void goBack(){ if(screen==1||screen==2||screen==3||screen==4||screen==7)showHome(); else if(screen==5)showHistory(); else if(screen==6){if(studentReturnScreen==7)showStats();else showRoundDetail();} else finish(); }
    @Override public void onBackPressed(){goBack();}
    String safe(String s){return s==null||s.trim().isEmpty()?"Sin matrícula":s;}

    static class Group{long id;String name;Group(long i,String n){id=i;name=n;}}
    static class Student{long id,groupId;String matricula,name;Student(long i,long g,String m,String n){id=i;groupId=g;matricula=m;name=n;}}
    static class Participation{long studentId;String name,matricula,result,time;Participation(long s,String n,String m,String r,String t){studentId=s;name=n;matricula=m;result=r;time=t;}}
    static class RoundRow{long id,groupId;String groupName,label;int total,p,n,a;}
    static class StatRow{long studentId;String name,matricula;int p,n,a;}

    static class DB extends SQLiteOpenHelper{
        static final String NAME="participaciones.db"; static final int VER=1; final Context ctx;
        DB(Context c){super(c,NAME,null,VER);ctx=c;}
        @Override public void onCreate(SQLiteDatabase d){
            d.execSQL("CREATE TABLE groups_tbl(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL UNIQUE)");
            d.execSQL("CREATE TABLE students(id INTEGER PRIMARY KEY AUTOINCREMENT,group_id INTEGER NOT NULL,matricula TEXT DEFAULT '',name TEXT NOT NULL,UNIQUE(group_id,matricula,name))");
            d.execSQL("CREATE TABLE rounds(id INTEGER PRIMARY KEY AUTOINCREMENT,group_id INTEGER NOT NULL,label TEXT NOT NULL,started_at TEXT NOT NULL,ended_at TEXT,active INTEGER NOT NULL DEFAULT 1)");
            d.execSQL("CREATE TABLE participations(id INTEGER PRIMARY KEY AUTOINCREMENT,round_id INTEGER NOT NULL,group_id INTEGER NOT NULL,student_id INTEGER NOT NULL,result TEXT NOT NULL,created_at TEXT NOT NULL)");
            seed(d);
        }
        @Override public void onUpgrade(SQLiteDatabase d,int o,int n){}
        String now(){return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.getDefault()).format(new Date());}
        String dateLabel(){return "Ronda del "+new SimpleDateFormat("dd/MM/yyyy",Locale.getDefault()).format(new Date());}
        long addGroup(String name){ContentValues v=new ContentValues();v.put("name",name);try{return getWritableDatabase().insertOrThrow("groups_tbl",null,v);}catch(Exception e){return -1;}}
        ArrayList<Group> groups(){ArrayList<Group>a=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT id,name FROM groups_tbl ORDER BY name COLLATE NOCASE",null);while(c.moveToNext())a.add(new Group(c.getLong(0),c.getString(1)));c.close();return a;}
        int studentCount(long g){return oneInt("SELECT COUNT(*) FROM students WHERE group_id=?",g);}
        int oneInt(String q,long x){Cursor c=getReadableDatabase().rawQuery(q,new String[]{String.valueOf(x)});int n=0;if(c.moveToFirst())n=c.getInt(0);c.close();return n;}
        int oneInt2(String q,long x,String y){Cursor c=getReadableDatabase().rawQuery(q,new String[]{String.valueOf(x),y});int n=0;if(c.moveToFirst())n=c.getInt(0);c.close();return n;}
        long ensureRound(long g){Cursor c=getReadableDatabase().rawQuery("SELECT id FROM rounds WHERE group_id=? AND active=1 ORDER BY id DESC LIMIT 1",new String[]{String.valueOf(g)});long id=-1;if(c.moveToFirst())id=c.getLong(0);c.close();if(id>0)return id;ContentValues v=new ContentValues();v.put("group_id",g);v.put("label",dateLabel());v.put("started_at",now());return getWritableDatabase().insert("rounds",null,v);}
        void restartRound(long g){SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{ContentValues end=new ContentValues();end.put("active",0);end.put("ended_at",now());d.update("rounds",end,"group_id=? AND active=1",new String[]{String.valueOf(g)});ContentValues v=new ContentValues();v.put("group_id",g);v.put("label",dateLabel());v.put("started_at",now());d.insert("rounds",null,v);d.setTransactionSuccessful();}finally{d.endTransaction();}}
        String roundLabel(long r){Cursor c=getReadableDatabase().rawQuery("SELECT label FROM rounds WHERE id=?",new String[]{String.valueOf(r)});String s="Ronda";if(c.moveToFirst())s=c.getString(0);c.close();return s;}
        long activeRound(long g){Cursor c=getReadableDatabase().rawQuery("SELECT id FROM rounds WHERE group_id=? AND active=1 ORDER BY id DESC LIMIT 1",new String[]{String.valueOf(g)});long id=-1;if(c.moveToFirst())id=c.getLong(0);c.close();return id;}
        int pendingCount(long g){long r=activeRound(g);if(r<0)return studentCount(g);Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM students s WHERE s.group_id=? AND NOT EXISTS(SELECT 1 FROM participations p WHERE p.round_id=? AND p.student_id=s.id AND p.result IN('P','N'))",new String[]{String.valueOf(g),String.valueOf(r)});int n=0;if(c.moveToFirst())n=c.getInt(0);c.close();return n;}
        int consumedCount(long r){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(DISTINCT student_id) FROM participations WHERE round_id=? AND result IN('P','N')",new String[]{String.valueOf(r)});int n=0;if(c.moveToFirst())n=c.getInt(0);c.close();return n;}
        int resultCount(long r,String res){return oneInt2("SELECT COUNT(*) FROM participations WHERE round_id=? AND result=?",r,res);}
        ArrayList<Student> pendingStudents(long g){long r=ensureRound(g);ArrayList<Student>a=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT s.id,s.group_id,s.matricula,s.name FROM students s WHERE s.group_id=? AND NOT EXISTS(SELECT 1 FROM participations p WHERE p.round_id=? AND p.student_id=s.id AND p.result IN('P','N')) ORDER BY s.name COLLATE NOCASE",new String[]{String.valueOf(g),String.valueOf(r)});while(c.moveToNext())a.add(new Student(c.getLong(0),c.getLong(1),c.getString(2),c.getString(3)));c.close();return a;}
        Student student(long id){Cursor c=getReadableDatabase().rawQuery("SELECT id,group_id,matricula,name FROM students WHERE id=?",new String[]{String.valueOf(id)});Student s=null;if(c.moveToFirst())s=new Student(c.getLong(0),c.getLong(1),c.getString(2),c.getString(3));c.close();return s;}
        void addParticipation(long r,long g,long s,String res){ContentValues v=new ContentValues();v.put("round_id",r);v.put("group_id",g);v.put("student_id",s);v.put("result",res);v.put("created_at",now());getWritableDatabase().insert("participations",null,v);}
        int studentResultCount(long s,String res){return oneInt2("SELECT COUNT(*) FROM participations WHERE student_id=? AND result=?",s,res);}
        ArrayList<Participation> participations(long r){ArrayList<Participation>a=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT p.student_id,s.name,s.matricula,p.result,p.created_at FROM participations p JOIN students s ON s.id=p.student_id WHERE p.round_id=? ORDER BY p.id",new String[]{String.valueOf(r)});while(c.moveToNext())a.add(new Participation(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4)));c.close();return a;}
        ArrayList<Participation> studentHistory(long s){ArrayList<Participation>a=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT p.student_id,st.name,st.matricula,p.result,p.created_at FROM participations p JOIN students st ON st.id=p.student_id WHERE p.student_id=? ORDER BY p.id DESC",new String[]{String.valueOf(s)});while(c.moveToNext())a.add(new Participation(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4)));c.close();return a;}
        ArrayList<RoundRow> roundsAll(){ArrayList<RoundRow>a=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT r.id,r.group_id,g.name,r.label,COUNT(p.id),SUM(CASE WHEN p.result='P' THEN 1 ELSE 0 END),SUM(CASE WHEN p.result='N' THEN 1 ELSE 0 END),SUM(CASE WHEN p.result='A' THEN 1 ELSE 0 END) FROM rounds r JOIN groups_tbl g ON g.id=r.group_id LEFT JOIN participations p ON p.round_id=r.id GROUP BY r.id ORDER BY r.id DESC",null);while(c.moveToNext()){RoundRow x=new RoundRow();x.id=c.getLong(0);x.groupId=c.getLong(1);x.groupName=c.getString(2);x.label=c.getString(3);x.total=c.getInt(4);x.p=c.getInt(5);x.n=c.getInt(6);x.a=c.getInt(7);a.add(x);}c.close();return a;}
        RoundRow round(long id){Cursor c=getReadableDatabase().rawQuery("SELECT r.id,r.group_id,g.name,r.label,COUNT(p.id),SUM(CASE WHEN p.result='P' THEN 1 ELSE 0 END),SUM(CASE WHEN p.result='N' THEN 1 ELSE 0 END),SUM(CASE WHEN p.result='A' THEN 1 ELSE 0 END) FROM rounds r JOIN groups_tbl g ON g.id=r.group_id LEFT JOIN participations p ON p.round_id=r.id WHERE r.id=? GROUP BY r.id",new String[]{String.valueOf(id)});RoundRow x=null;if(c.moveToFirst()){x=new RoundRow();x.id=c.getLong(0);x.groupId=c.getLong(1);x.groupName=c.getString(2);x.label=c.getString(3);x.total=c.getInt(4);x.p=c.getInt(5);x.n=c.getInt(6);x.a=c.getInt(7);}c.close();return x;}
        ArrayList<StatRow> statsByPeriod(long g,String from,String to){ArrayList<StatRow>a=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT s.id,s.name,s.matricula,SUM(CASE WHEN p.result='P' THEN 1 ELSE 0 END),SUM(CASE WHEN p.result='N' THEN 1 ELSE 0 END),SUM(CASE WHEN p.result='A' THEN 1 ELSE 0 END) FROM students s LEFT JOIN participations p ON p.student_id=s.id AND substr(p.created_at,1,10)>=? AND substr(p.created_at,1,10)<=? WHERE s.group_id=? GROUP BY s.id ORDER BY 4 DESC, s.name COLLATE NOCASE",new String[]{from,to,String.valueOf(g)});while(c.moveToNext()){StatRow r=new StatRow();r.studentId=c.getLong(0);r.name=c.getString(1);r.matricula=c.getString(2);r.p=c.getInt(3);r.n=c.getInt(4);r.a=c.getInt(5);a.add(r);}c.close();return a;}
        String studentsText(long g){StringBuilder b=new StringBuilder();Cursor c=getReadableDatabase().rawQuery("SELECT matricula,name FROM students WHERE group_id=? ORDER BY name COLLATE NOCASE",new String[]{String.valueOf(g)});while(c.moveToNext())b.append(c.getString(0)).append(" | ").append(c.getString(1)).append("\n");c.close();return b.toString();}
        int importStudents(long g,String text){int count=0;SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{for(String line:text.split("\\r?\\n")){line=line.trim();if(line.isEmpty())continue;String m="",n=line;String[] p=line.split("\\|",2);if(p.length==2){m=p[0].trim();n=p[1].trim();}else{p=line.split("\\t",2);if(p.length==2){m=p[0].trim();n=p[1].trim();}}if(n.isEmpty())continue;ContentValues v=new ContentValues();v.put("group_id",g);v.put("matricula",m);v.put("name",n.toUpperCase());long id=d.insertWithOnConflict("students",null,v,SQLiteDatabase.CONFLICT_IGNORE);if(id==-1 && !m.isEmpty()){ContentValues u=new ContentValues();u.put("name",n.toUpperCase());d.update("students",u,"group_id=? AND matricula=?",new String[]{String.valueOf(g),m});}count++;}d.setTransactionSuccessful();}finally{d.endTransaction();}return count;}
        void deleteGroup(long g){SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{d.delete("participations","group_id=?",new String[]{String.valueOf(g)});d.delete("rounds","group_id=?",new String[]{String.valueOf(g)});d.delete("students","group_id=?",new String[]{String.valueOf(g)});d.delete("groups_tbl","id=?",new String[]{String.valueOf(g)});d.setTransactionSuccessful();}finally{d.endTransaction();}}
        void seed(SQLiteDatabase d){
            seedGroup(d,"1AS",new String[]{
"1533|AGUIRRE QUIÑONEZ MATIAS EDUARDO","1785|BEDOYA OCAMPO SAMUEL","1140|BRICEÑO PEÑUELAS JOSE FRANCISCO","1913|CASTRO OSUNA IZAK","1420|CEJUDO GONZALEZ DE LA LLAVE JOSE MANUEL","1920|CHAMA PALAFOX CAMILA","1421|CONDE ALCANTAR MIRANDA ABIGAIL","1967|CORVERA ALVARADO MELANI GUADALUPE","1360|COSIO RIOS ISAAC","1924|GAMEZ URIAS RENATA NAHOMY","1941|GREEN GERALDO LEONARDO","1228|HERNANDEZ FLORES EDGAR ALEXANDER","1365|IBARRA CARRASCO ADRIANA MICHELLE","1995|LOPEZ HERNANDEZ NIKOL","1946|MANRIQUEZ CASAS MARCO JOSEFATH","1374|MENDEZ SEGOVIA MIRANDA SOPHIA","1116|MORALES SANCHEZ CASSANDRA YENEDITH","2004|MOYRON ARELLANO HANNYA RENATA","2003|ORTIZ CASTRO LUNA CAMILLE","1350|OSUNA ALCANTARA CHLOE AKEMI","1927|RAMIREZ MONTAÑO IAN CANEK","2019|RIVERA RUVALCABA YADIEL HUMBERTO","1189|RODRIGUEZ HUERTA JUAN EMILIANO","2014|RUIZ FLORES GAEL","2015|RUIZ FLORES ISAAC","1906|SANCHEZ SANDEZ MIGUEL ANGEL","1589|VELEZ MARQUEZ MILDRETH","1664|ZAMORA ACEVEDO JOSELINE EVANGELY","1881|ZAYAS GUTIERREZ CLAUDIA AMELI"});
            seedGroup(d,"1BS",new String[]{
"1227|ANGULO GERALDO GUSTAVO","2017|BAÑUELOS MEJIA MELANY VALENTINA","1951|BURGOIN LOPEZ JOSE FERNANDO","1975|CARBALLO GELACIO VALENTINA","1044|CARRILLO OCEGUERA RAFAEL","1919|CHAMA PALAFOX XIMENA","1966|CONTRERAS TRASVIÑA ALEJANDRO","1932|COSIO SEPULVEDA REGINA","1020|DE LA VEGA GARCIA AMELIE","1168|ESTRADA MENDIVIL EVANS GABRIEL","1613|FUENTEVILLA TENORIO ANGEL CIRO","1392|GARCIA ALMANZA ISABELLA FERNANDA","1982|GARCIA TRASVIÑA JESUS AARON","1925|IGLESIAS MEJIA MIA MICHELLE","1212|JIMENEZ MICHEL DANIA ODETTE","1384|LEMUS AMADOR GIANNA ANDREA","1953|LEON CESENA BEATRIZ ALIXIS","1935|LOPEZ ZAVALA MATEO","1730|MARTINEZ AÑORVE MIA SCARLETT","1690|MEZA ARREDONDO MARIANA ELIZABETH","1952|NARCE VENTURA MARIA FERNANDA","1165|OJEDA ALONSO YADIEL ALEXANDER","1937|RAMIREZ COTA RENAN EDUARDO","1264|REYES MARTINEZ JUAN JOSE","1939|SANCHEZ OLGUIN MIA AIRAM","1691|VAZQUEZ HILARIO YARITZI","1575|VILLAVICENCIO RUAN DRIAM ONEL","1916|ZARATE SALAZAR MARCOS ALBERTO"});
            seedGroup(d,"2AS",new String[]{
"925|ALVARADEJO SILVESTRE MARIA XIMENA","1878|ARELLANO SOTO SANTIAGO ALBERTO","865|AVILES CONTRERAS JOSE LEONARDO","1810|AVILES TAMAYO SARAH KARELY","1611|CABALLERO COTA JUAN FRANCISCO","1043|CARRILLO DOMINGUEZ DAVID","1056|CASTILLO VELAZCO ALEXANDER","1261|CASTRO GOMEZ MARAH YISEL","1280|COLLINS ROJAS JORGE ENRIQUE","1663|COSIO VILLELA ALESSIA","1526|GAMEZ AGRUEL JOEL ABRAHAM","1026|GOMEZ GERARDO SOFIA","1873|GONZALEZ ROBLES DAFNEE VALENTINA","1050|GONZALEZ SERNA ANDREA DENISSE","1746|HAFENECKER-DODGE OROZCO MONEO ALEXANDER","935|HERNÁNDEZ SANDOVAL ADELYN ANDREA","1639|JIMENEZ COTA VICTORIA ISABELLA","1698|LEON ALVAREZ CRISTOBAL ANTONIO","894|MARQUEZ CARBALLO DIEGO ANDRE","1852|MEZA LARA DYLAN","1283|MOLINA TALAMANTES ZOE VALENTINA","1871|OSUNA SANCHEZ NICOLE","1017|PEREZ COTA VALENTINA ESTHER","1564|RODRIGUEZ CORTEZ PABLO","1243|ROJAS BAUTISTA SARA SOFIA","1792|ROSAS BURGOIN JIMENA GUADALUPE","1053|SAUCEDA SOTO REGINA","1999|VALADEZ MARTINEZ AFRICA YAMILE","1273|VALLE FAMANIA MARIANGELES","1662|VERDUZCO ALVAREZ CAMILA GUADALUPE"});
            seedGroup(d,"3AS",new String[]{
"1148|AMADOR VALDEZ CARLOS RAUL","1512|ASPURU MANRIQUEZ EDUARDO","828|CARRILLO DOMINGUEZ DANIEL","888|CARRILLO OCEGUERA CRISTOPHER EDUARDO","1159|CHAVEZ VAZQUEZ VALERIA","1130|COTA CHEE ALINA IDALID","892|COTA COTA EDWIN","1563|GALVAN OCAMPO MIA MICHEL","1825|GOMEZ GARCIA GABRIELA VALENTINA","1119|GUILLEN SIMEON RAMON OZIEL","737|GÜEREÑA VALDEZ MAIRIM","881|IBAÑEZ VELAZQUEZ SEBASTIAN","1687|LOPEZ FUENTES KEVYN YAEL","1042|LOPEZ RUIZ EVELIN","1689|MEZA ARREDONDO MARIEL FERNANDA","902|MIRANDA COTA DIEGO ALEJANDRO","852|NUÑEZ YEPIZ ALEXIS ANDRES","1406|OROZCO BERTRAN VALENTINA GUADALUPE","1705|OROZCO CAMACHO DIDIER XAVIER ALEXANDER","1706|OROZCO CAMACHO DORIAN FERNANDO ANDRE","1725|ORTEGA GONZALEZ SOFIA IRISABETH","1583|PEREZ CASTRO FATIMA ANGELINE","840|QUIROZ OLIVARES ABY JANETH","1991|RAMIREZ JUAREZ MAXIM NICOLE","1247|ROJAS BAUTISTA ERICA YVONNE","976|RUIZ RAMOS GAEL","989|SANCHEZ SOTO MELISSA","1859|URIBE ALDANA JOSE ROBERTO","1652|VALDEZ MORALES SEBASTIAN","1726|VIZCAINO ROMERO CRISTOPHER ALAIN"});
        }
        void seedGroup(SQLiteDatabase d,String name,String[] students){ContentValues g=new ContentValues();g.put("name",name);long gid=d.insert("groups_tbl",null,g);for(String x:students){String[] p=x.split("\\|",2);ContentValues v=new ContentValues();v.put("group_id",gid);v.put("matricula",p[0]);v.put("name",p[1]);d.insert("students",null,v);}}
    }
}
