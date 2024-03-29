unit dcalc;

interface

uses
    SysUtils,
    bravo4a,
    city10 ;

procedure init_dcalc;
procedure deinit_dcalc;
function GetDistance(adr1, adr2: shortstring): real;
function CreateStreetsTxt : shortstring;


var
   city  : TCity;
   bravo : TBravo;

implementation
uses TpString;
type
    cRec = record
         cs : shortstring;
         d  : real;
     end; // record;

const
   maxC = 2000;

var
   CTable : array[0..maxC] of cRec;
   AddPos : longint = 0;

function cool_round(i:real):real;
var res : integer;
begin
//  if i <= 1000 then begin Result:=1000;exit;end;
  res:=round(i) div 1000;
  res:=res*1000;
  result:=res;
end;

procedure incAddPos;
begin
 inc(AddPos);
 if AddPos=maxC+1 then AddPos:=0;
end;

function CreateStreetsTxt : shortstring;
var f : textfile;
    count,rr : longint;
    r : tRef;
begin
 assignfile(f,'streets.txt'); Rewrite(f);
 count:=city.List.Count;
 dec(count);
 for rr:=0 to count do
  begin
   r:=city.List.Items[rr];
   writeln(f,r.str);
  end;
 closefile(f);
end;

function GetCoord(adr: string;var f : boolean): TCoord;
var
  r: TRef;
  l: TLine;
begin
  r:=city.List.Identify(adr);
  if r is TLine then
    l:=r as TLine
  else
    l:=nil;

   if l<>nil
     then
      begin
       Result:=l.Identify(adr);
       f:=true;
      end
     else
      begin
       f := false;
      end;
end;

function get_plus(s: string):longint;
var i,rr,l : longint;
    to_val : string;
    code : integer;
begin
  s:=s+' ';
  Result:=0;
  i:= Pos('+',s);
  if i<>0 then
   begin
    l:=length(s);
    to_val:='';
    while (s[i]<>' ') and (i<=l) do
     begin
      to_val:=to_val+s[i];
      inc(i);
     end;
    delete(to_val,1,1);
    val(to_val,rr,code);
    if code<>0 then exit;
    Result:=rr;
   end;
end;
function get_plus_all(s: string):longint;
var
   wc,rr,summa : longint;
begin
  summa:=0;
  wc:=WordCount(s,['+']);
  if wc<>0 then
  for rr:= 1 to wc do
   begin
    summa:=summa+get_plus('+'+ExtractWord(rr,s,['+']));
   end;
  Result:=Summa;
end;

function findCashe(s:string):real;
var
  rr : longint;
begin
  result:=-1;
  for rr:= 0 to maxC do
   begin
    if CTable[rr].cs = s then
     begin
      result:=CTable[rr].d;
      break;
     end;
   end;
end;

procedure AddToCashe(S:shortstring;d:real);
begin
 if findCashe(s) = -1 then
  begin
   CTable[AddPos].cs:=s;
   CTable[AddPos].d:=d;
   IncAddPos;
  end;
end;

function GetDistance(adr1, adr2: shortstring): real;
var
  p1,p2: TCoord;
  f : boolean;
  dd,fc : real;
begin
  fc:=findCashe(adr1+' '+adr2);
  if fc <> -1 then
   begin
    result:=fc;
    exit;
   end;
  p1:=GetCoord(adr1,f); if f = false then begin result:=-1 ; exit;end;
  p2:=GetCoord(adr2,f); if f = false then begin result:=-1 ; exit;end;
  dd:=bravo.Trace(trunc(p1.x), trunc(p1.y), trunc(p2.x), trunc(p2.y));  //� ������!!!
  dd:=dd+get_plus_all(adr1+' '+adr2);
  //dd:=cool_round(dd);
  AddToCashe(adr1+' '+adr2,dd);
  result:=dd;
//  if Result=-1 then
//  ShowMessage('�� ������� ��������� ���������� - �������� ����� �������� �������');
end;


procedure init_dcalc;
var
   rr : longint;
begin
  city:=TCity.Create;
  city.Load('lines.xml');
  bravo:=TBravo.Create;
  bravo.load(city.list);    //��� ���� ���� �������� 15 ������ �� 1��� �����
  bravo.Make;
  for rr:=0 to maxC do
   begin
    cTable[rr].cs:='';
    cTable[rr].d:=-1;
   end;
end;

procedure deinit_dcalc;
begin
 city.Free;
 bravo.Free;
end;


end.
