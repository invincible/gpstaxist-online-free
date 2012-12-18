unit main;

interface

uses
  Windows, Messages, SysUtils, Variants, Classes, Graphics, Controls, Forms,
  Dialogs, StdCtrls, IdBaseComponent, IdComponent, IdUDPBase, IdUDPClient,
  IdUDPServer,
  IdSocketHandle, IdTCPServer, Sockets,dcalc,TpString; //!!!!!!!!!!!

type
  TForm1 = class(TForm)
    EditPort: TEdit;
    Label4: TLabel;
    MemoData: TMemo;
    Label1: TLabel;
    TcpServer1: TTcpServer;
    Button1: TButton;
    Edit1: TEdit;
    procedure EditPortChange(Sender: TObject);
    procedure IdUDPServer1UDPRead(Sender: TObject; AData: TStream;
      ABinding: TIdSocketHandle);
    procedure TcpServer1Accept(Sender: TObject;
      ClientSocket: TCustomIpClient);
    procedure FormCreate(Sender: TObject);
    procedure FormDestroy(Sender: TObject);
    procedure Button1Click(Sender: TObject);
  private
    { Private declarations }
  public
    { Public declarations }
  end;

    // you must create your own thread to synch
  // writing to a gui component
  TClientDataThread = class(TThread)
  private
  public
    ListBuffer: TStringList;
    TargetList: TStrings;
    procedure synchAddDataToControl;
    constructor Create(CreateSuspended: Boolean);
    procedure Execute; override;
    procedure Terminate;
  end;

var
  Form1: TForm1;
  WM_NEWORDER: cardinal;
  f : textfile;

const
  fn = 'rec.log';
implementation

{$R *.dfm}
//Var  WorkMode : longint;
const
   WorkOne  = 1;
   WorkPack = 2;
   CharDelims = '"';
procedure AddToFile(S:string);
begin
 assignfile(f,fn);

 if SysUtils.FileExists(fn) then Append(f)
                            else ReWrite(f);
 Writeln(f,s);
 CloseFile(f);
end;
//------------- TClientDataThread impl -----------------------------------------

constructor TClientDataThread.Create(CreateSuspended: Boolean);
begin
//  inherited Create(CreateSuspended);
  FreeOnTerminate := true;
  ListBuffer := TStringList.Create;
  inherited Create(CreateSuspended);
end;

procedure TClientDataThread.Terminate;
begin
  ListBuffer.Free;
  inherited;
end;

procedure TClientDataThread.Execute;
begin
  Synchronize(synchAddDataToControl);
  //StopMode:=smTerminate;
  Terminate;
  sleep(0);
end;

procedure TClientDataThread.synchAddDataToControl;
begin
  TargetList.Clear;
  TargetList.AddStrings(ListBuffer);
end;
//------------- end TClientDataThread impl -------------------------------------


procedure TForm1.EditPortChange(Sender: TObject);
begin
  TcpServer1.Active:=False;
  TcpServer1.LocalPort:=EditPort.Text;
  TcpServer1.Active:=True;
end;

procedure TForm1.IdUDPServer1UDPRead(Sender: TObject; AData: TStream;
  ABinding: TIdSocketHandle);
var
  s: string;
begin
  SetLength(s, AData.Size);
  AData.Read(s[1], AData.Size);
  MemoData.Lines.Text := s;
end;

Function GetPAcket(var startPos : longint;Strok:string):string;
var oStr : string;
begin
ostr:='';
if startPos>Length(strok) then
 begin
  Result:='';
  inc(StartPos);
  exit;
 end;
while StrOk[StartPos]<>CharDelims do inc(startPos);
inc(StartPos);
while StrOk[StartPos]<>CharDelims
do begin
    ostr:=ostr+StrOk[StartPos];
    inc(StartPos);
   end;
// ostr:=Trim(ostr);
 Result:=Trim(ostr);
 inc(StartPos);
end;

var
    sema : boolean = false;
procedure TForm1.TcpServer1Accept(Sender: TObject;
  ClientSocket: TCustomIpClient);
var
  //s: string;
  DataThread: TClientDataThread;
  sMode,sStr1, sStr2, sDom1, sDom2,POut,s1,s2,d1,d2: string;//shortstring;
  PosStr1,PosDom1,PosStr2,PosDom2,CountBases : longint;
  Distance: integer;
  WorkMode : longint;

const
  wt = 2000;

begin
  if ClientSocket.WaitForData(wt)=false then begin sema:=false; exit; end;
  repeat
  until sema=false;
  sema:=true;
  CountBases:=0;
  PosStr1:=1;
  PosDom1:=1;
  PosStr2:=1;
  PosDom2:=1;
  // create thread
  DataThread := TClientDataThread.Create(true);
  // set the TagetList to the gui list that you
  // with to synch with.

  DataThread.TargetList := MemoData.lines;

  // Load the Threads ListBuffer

///  DataThread.ListBuffer.Add('*** Connection Accepted ***');
///  DataThread.ListBuffer.Add('Remote Host: ' + ClientSocket.LookupHostName(ClientSocket.RemoteHost) +
///    ' (' + ClientSocket.RemoteHost + ')');
///  DataThread.ListBuffer.Add('===== Begin message =====');
  if ClientSocket.WaitForData(wt)=false then begin sema:=false; exit; end;
  sMode := ClientSocket.Receiveln;
  if ClientSocket.WaitForData(wt)=false then begin sema:=false; exit; end;
  sStr1 := ClientSocket.Receiveln;
  if ClientSocket.WaitForData(wt)=false then begin sema:=false; exit; end;
  sDom1 := ClientSocket.Receiveln;
  if ClientSocket.WaitForData(wt)=false then begin sema:=false; exit; end;
  sStr2 := ClientSocket.Receiveln;
  if ClientSocket.WaitForData(wt)=false then begin sema:=false; exit; end;
  sDom2 := ClientSocket.Receiveln;
  AddToFile(DateTimeToStr(now)+' '+sMode);
//  AddToFile(sStr1);
//  AddToFile(sDom1);
//  AddToFile(sStr2);
//  AddToFile(sDom2);

  if sMode='001' then WorkMode:=WorkOne
  else
  if sMode='002' then WorkMode:=WorkPack;

//  DataThread.ListBuffer.Add(sMode);
//  DataThread.ListBuffer.Add(sStr1);
//  DataThread.ListBuffer.Add(sDom1);
//  DataThread.ListBuffer.Add(sStr2);
//  DataThread.ListBuffer.Add(sDom2);

///  DataThread.ListBuffer.Add('===== End of message =====');

  //////////////// YOUR CALCULATIONS ///////////////////////////
  //Distance := random(1000);
  //Caption := 'Distance ' + IntToStr(Distance);
 case WorkMode of
 WorkOne:
  begin
  sleep(0);
  if Trim(sStr1+' '+sDom1)='' then
  Distance:=-1
  else
  if Trim(sStr2+' '+sDom2)='' then
  Distance:=-1
  else
  if Trim(sStr2+' '+sDom2)=Trim(sStr1+' '+sDom1) then
  Distance:=0
  else

  //if sStr1 = 'Улица (откуда)'
  //  then Distance:=-1
    //else
    Distance := trunc(GetDistance(sStr1+' '+sDom1,sStr2+' '+sDom2));
///  Caption := 'Distance ' + IntToStr(Distance);
//  AddTofile('========='+DateTimeToStr(now));
//  AddToFile(DateTimeToStr(now)+' '+sMode+''+sStr1+' '+sDom1+' '+sStr2+' '+sDom2);
//  AddToFile(IntToStr(Distance));
  //////////////// YOUR CALCULATIONS ///////////////////////////

  ClientSocket.Sendln(IntToStr(distance));
  end;
 WorkPack:
  begin
 //  sleep(0);
   AddTofile('Packet Start time = '+DateTimeToStr(now));
   AddTofile(sStr1);
   AddTofile(sDom1);
   AddTofile(sStr2);
   AddTofile(sDom2);
   POut:='';
   while PosStr1<Length(sStr1) do
    begin
     s1:=GetPAcket(PosStr1,sStr1);
     s2:=GetPAcket(PosStr2,sStr2);
     d1:=GetPAcket(PosDom1,sDom1);
     d2:=GetPAcket(PosDom2,sDom2);
//     sleep(0);
     if s1+d1 =''  then Distance:=-1
      else
     if s2+d2 =''  then Distance:=-1
      else
       begin
        inc(countbases);
        Distance:=trunc(GetDistance(s1+' '+D1,s2+' '+D2));
       end;
     if Distance=-1 then Distance:=0;
     POut:=POut+'"0'+IntToStr(Distance)+'",';
    end;
   SetLength(POut,Length(POut)-1);
   AddTofile(POut);
   ClientSocket.Sendln(POut);
   AddTofile('Packet End time = '+DateTimeToStr(now)+' Count='+IntToStr(CountBAses));
  end;
  else sema:=false;
 end;// case
  // Call Resume which will execute and synch the
  // ListBuffer with the TargetList
  DataThread.Resume;

 sema:=false;
end;

procedure TForm1.FormCreate(Sender: TObject);
begin
  init_dcalc;
  TcpServer1.LocalPort := EditPort.Text;
  TcpServer1.Active := True;
end;

procedure TForm1.FormDestroy(Sender: TObject);
begin
 deinit_dcalc;
end;

procedure TForm1.Button1Click(Sender: TObject);
begin
 Edit1.Text:=CreateStreetsTxt;
end;

end.

