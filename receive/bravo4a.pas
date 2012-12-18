//(C) 2003-2005  yusupov@gmail.com
unit bravo4a;

interface

uses
  Classes,
  city10;

type
  TBravoPoint = class
  private
    l: TList;
    procedure SetLink(index: Integer; value: TBravoPoint);
    function GetLink(index: Integer): TBravoPoint;
  public
    x,y: Float;
    r: Float;
    property Links[index: Integer]: TBravoPoint read GetLink write SetLink;
    function LinksCount: Integer;
    procedure Connect(t: TBravoPoint);
    procedure Disconnect(index: Integer);
    procedure LinkBreak(link: TBravoPoint; b: TBravoPoint);
    function LinkIndex(b: TBravoPoint): Integer;
    constructor Create(vx, vy: Float);
    destructor Destroy;  override;
  end;

  TBravo = class
  private
    m: TList;
    procedure SetItem(index: Integer; value: TBravoPoint);
    function GetItem(index: Integer): TBravoPoint;
  public
    property Items[index: Integer]: TBravoPoint read GetItem write SetItem;
    function Add(x, y: Float): TBravoPoint;
    procedure Load(Lines: TCityList);
    procedure Make;
    function IndexOf(a: TBravoPoint): Integer;
    procedure Clear;
    function Count: Integer;
    function Trace(x1,y1, x2,y2: Integer): Real;
    procedure ZeroBravo;
    procedure DeepFirst(p: TBravoPoint; r: Float);
    procedure Fill(e: Integer);  overload;
    procedure Fill;  overload;
    function SelectNearest(x,y: Float): Integer;
    procedure Save(fname: string);
    constructor Create;
    destructor Destroy;  override;
  end;

implementation

uses
  SysUtils;

procedure TBravoPoint.SetLink(index: Integer; value: TBravoPoint);
begin
  l[index]:=value;
end;

function TBravoPoint.GetLink(index: Integer): TBravoPoint;
begin
  Result:=l[index];
end;

function TBravoPoint.LinksCount: Integer;
begin
  Result:=l.Count;
end;

function TBravoPoint.LinkIndex(b: TBravoPoint): Integer;
begin
  Result:=l.IndexOf(b);
end;

procedure TBravoPoint.Connect(t: TBravoPoint);
begin
  l.Add(t);
  t.l.Add(Self);
end;

procedure TBravoPoint.Disconnect(index: Integer);
begin
  Links[index].l.Delete(Links[index].LinkIndex(Self));
  l.Delete(index);
end;

procedure TBravoPoint.LinkBreak(link: TBravoPoint; b: TBravoPoint);
begin
  Disconnect(LinkIndex(link));
  Connect(b);
  link.Connect(b);
end;

constructor TBravoPoint.Create(vx, vy: Float);
begin
  inherited Create;
  l:=TList.Create;
  x:=vx;
  y:=vy;
end;

destructor TBravoPoint.Destroy;
begin
  l.Free;
  inherited Destroy;
end;

procedure TBravo.SetItem(index: Integer; value: TBravoPoint);
begin
  m.Items[index]:=value;
end;

function TBravo.GetItem(index: Integer): TBravoPoint;
begin
  Result:=m.Items[index];
end;

procedure TBravo.ZeroBravo;
var
  i: Integer;
begin
  for i:=0 to Count-1 do
    Items[i].r:=0;
end;

function TBravo.Add(x, y: Float): TBravoPoint;
begin
  Result:=m.Items[m.Add(TBravoPoint.Create(x, y))];
end;

constructor TBravo.Create;
begin
  inherited Create;
  m:=TList.Create;
end;

destructor TBravo.Destroy;
begin
  Clear;
  m.Free;
  inherited Destroy;
end;

procedure TBravo.Clear;
var
  i: Integer;
begin
  for i:=Count-1 downto 0 do begin
    Items[i].Free;
    m.Delete(i);
  end;
end;

function TBravo.Count: Integer;
begin
  Result:=m.Count;
end;

function TBravo.IndexOf(a: TBravoPoint): Integer;
begin
  Result:=m.IndexOf(a);
end;

procedure TBravo.Save(fname: string);
var
  f: TextFile;
  i,j: Integer;
begin
  AssignFile(f, fname);
  Rewrite(f);
  for i:=0 to Count-1 do begin
    Write(f, IntToStr(i)+'. ('+IntToStr(Round(Items[i].x))+', '+IntToStr(Round(Items[i].y))+') [ ');
    for j:=0 to Items[i].LinksCount-1 do
      Write(f, IntToStr(IndexOf(Items[i].Links[j]))+' ');
    Write(f, ']');
    if Items[i].r>0 then
      Write(f, ' * ', IntToStr(Round(Items[i].r)));
    Writeln(f);
  end;
  CloseFile(f);
end;

procedure TBravo.Load(Lines: TCityList);

  procedure CreateBravo;
  var
    i, j: Integer;
    p1, p2: TBravoPoint;
    l: TLine;
  begin
    Clear;
    p1:=nil;
    for i:=0 to Lines.Count-1 do
      if (Lines[i] is TLine) then begin
        l:=(Lines[i] as TLine);
        for j:=0 to l.Vertexes.Count-1 do begin
          p2:=Add(l.Vertexes[j].x, l.Vertexes[j].y);
          if j>=1 then
            p2.Connect(p1);
          p1:=p2;
        end;
      end;
  end;

begin
  CreateBravo;
end;

procedure TBravo.Make;

  procedure SaveCtrl(a,b,c,d: Integer);
  var
    f: TextFile;
  begin
    AssignFile(f, '!cur.txt');
    Rewrite(f);
    Writeln(f, a, ' ', b, ' ', c, ' ', d);
    CloseFile(f);
  end;

  function CheckInLine(a, b: TBravoPoint; x, y: Float): Boolean;
  begin
    Result:=(
      (a.x<x)=not (b.x<x)
    ) and (
      (a.y<y)=not (b.y<y)
    );
  end;

  procedure BreakBravo;

    function CheckDivide(p1, p2, q1, q2: TBravoPoint): Boolean;

      function IsCanCross: Boolean;
      type
        tr = record
          x1,y1,x2,y2: Float;
        end;

        procedure GenBound(n, m: TBravoPoint; var r: tr);
        begin
          if n.x<m.x then begin
            r.x1:=n.x;  r.x2:=m.x;
          end else begin
            r.x2:=n.x;  r.x1:=m.x;
          end;
          if n.y<m.y then begin
            r.y1:=n.y;  r.y2:=m.y;
          end else begin
            r.y2:=n.y;  r.y1:=m.y;
          end;
        end;

      var
        a, b: tr;
        v: Boolean;
      begin
        GenBound(p1, p2, a);
        GenBound(q1, q2, b);
        v:=true;
        if (a.x1>b.x2) or (a.x2<b.x1) then  v:=false;
        if (a.y1>b.y2) or (a.y2<b.y1) then  v:=false;
        Result:=v;
      end;

      function IsOnePoint: Boolean;
      begin
        Result:=false;
        if
          (p1=q1) or (p1=q2) or
          (p2=q1) or (p2=q2)
        then
          Result:=true;
      end;

    var
      a1, b1: Float;  v1: Boolean;
      a2, b2: Float;  v2: Boolean;
      x,y: Float;  v: Boolean;
      newpoint: TBravoPoint;
    begin
      v1:=false;
      v2:=false;
      x:=0;  y:=0;  a1:=0;  a2:=0;
      Result:=false;
      if not IsOnePoint then
        if IsCanCross then begin
          if p2.x=p1.x then begin
            v1:=true;
            b1:=p1.x;
          end else begin
            a1:=(p2.y-p1.y)/(p2.x-p1.x);
            b1:=p1.y-a1*p1.x;
          end;
          if q2.x=q1.x then begin
            v2:=true;
            b2:=q1.x;
          end else begin
            a2:=(q2.y-q1.y)/(q2.x-q1.x);
            b2:=q1.y-a2*q1.x;
          end;
          if (not v1) and (not v2) then begin
            v:=(Abs(a1-a2)>=0.1);
            if v then begin
              x:=(b2-b1)/(a1-a2);
              y:=(a1*b2-a2*b1)/(a1-a2);
            end;
          end else begin
            v:=not (v1 and v2);
            if v then
              if v1 then begin
                x:=b1;
                y:=a2*x+b2;
              end else begin
                x:=b2;
                y:=a1*x+b1;
              end;
          end;
          if v then
            if CheckInLine(p1, p2, x, y) and CheckInLine(q1, q2, x, y) then begin
              newpoint:=Add(x, y);
              p1.LinkBreak(p2, newpoint);
              q1.LinkBreak(q2, newpoint);
              Result:=true;
            end;
        end;
    end;

    function ProceedLine(a1, b1: TBravoPoint): Boolean;
    var
      i2,j2: Integer;
    begin
      Result:=false;
      i2:=IndexOf(a1);
      repeat
        j2:=0;
        if Items[i2].LinksCount>0 then
          repeat
            if CheckDivide(a1, b1, Items[i2], Items[i2].Links[j2]) then begin
              Result:=true;
              i2:=Count-1;
              j2:=Items[i2].LinksCount;
            end;
            Inc(j2);
          until j2>=Items[i2].LinksCount;
        Inc(i2);
      until i2>=Count;
    end;

  var
    i1,j1: Integer;
  begin
    if Count>0 then begin
      i1:=0;
      repeat
        j1:=0;
        if Items[i1].LinksCount>0 then
          repeat
            ProceedLine(Items[i1], Items[i1].Links[j1]);
            Inc(j1);
          until j1>=Items[i1].LinksCount;
        Inc(i1);
      until i1>=Count;
    end;
  end;

  procedure WeldPoints(p1, p2: TBravoPoint);
  var
    i: Integer;
    p3,t: TBravoPoint;
  begin
    p3:=Add((p1.x+p2.x)/2, (p1.y+p2.y)/2);
    for i:=p1.LinksCount-1 downto 0 do
      if p1.Links[i]<>p2 then begin
        t:=p1.Links[i];
        p1.Disconnect(i);
        t.Connect(p3);
      end;
    for i:=p2.LinksCount-1 downto 0 do
      if p2.Links[i]<>p1 then begin
        t:=p2.Links[i];
        p2.Disconnect(i);
        t.Connect(p3);
      end;
    m.Delete(IndexOf(p1));
    m.Delete(IndexOf(p2));
    p1.Free;
    p2.Free;
  end;

  procedure WeldBravo(weldvalue: Float);
  var
    i,j: Integer;
  begin
    i:=0;
    if Count>0 then
      repeat
        j:=0;
        repeat
          if i<>j then
            if (Abs(Items[i].x-Items[j].x)+Abs(Items[i].y-Items[j].y)<weldvalue) then
              WeldPoints(Items[i], Items[j]);
          Inc(j);
        until (j>=Count) or (i>=Count);
        Inc(i);
      until i>=Count;
  end;

begin
  WeldBravo(0.5);
  BreakBravo;
  WeldBravo(3);
end;

function TBravo.Trace(x1,y1, x2,y2: Integer): Real;
var
  v: Boolean;
  m1,m2: Integer;
  rr: Float;
  i,j: Integer;
  p,q: TBravoPoint;
begin
  Result:=-1;
  ZeroBravo;
  m1:=SelectNearest(x1, y1);
  m2:=SelectNearest(x2, y2);
  if (m1<>-1) and (m2<>-1) then begin
    Items[m1].r:=1;
    repeat
      v:=false;
      for i:=0 to Count-1 do begin
        p:=Items[i];
        if p.r>0 then
          for j:=0 to p.LinksCount-1 do begin
            q:=p.Links[j];
            if (q.r=0) or (q.r>p.r) then begin
              rr:=Sqrt(Sqr(p.x-q.x)+Sqr(p.y-q.y));
              if (q.r>p.r+rr+1) or (q.r=0) then begin
                q.r:=p.r+rr;
                v:=true;
              end;
            end;
          end;
      end;
    until not v;
    if Items[m2].r<>0 then
      Result:=Items[m2].r-1;
  end;
end;

function TBravo.SelectNearest(x,y: Float): Integer;
var
  i,m: Integer;
  r,c: Float;
begin
  m:=-1;
  r:=0;
  for i:=0 to Count-1 do begin
    c:=Abs(Items[i].x-x)+Abs(Items[i].y-y);
    if (c<r) or (m=-1) then begin
      r:=c;
      m:=i;
    end;
  end;
  Result:=m;
end;

procedure TBravo.DeepFirst(p: TBravoPoint; r: Float);
var
  i: Integer;
  q: TBravoPoint;
begin
  if (p.r=0) or (p.r>r) then begin
    p.r:=r;
    for i:=0 to p.LinksCount-1 do begin
      q:=p.Links[i];
      DeepFirst(q, r+Sqrt(Sqr(q.x-p.x)+Sqr(q.y-p.y)));
    end;
  end;
end;

procedure TBravo.Fill;
var
  v: Boolean;
  rr: Float;
  i,j: Integer;
  p,q: TBravoPoint;
begin
  repeat
    v:=false;
    for i:=0 to Count-1 do begin
      p:=Items[i];
      if p.r>0 then
        for j:=0 to p.LinksCount-1 do begin
          q:=p.Links[j];
          if (q.r=0) or (q.r>p.r) then begin
            rr:=Sqrt(Sqr(p.x-q.x)+Sqr(p.y-q.y));
            if (q.r>p.r+rr+1) or (q.r=0) then begin
              q.r:=p.r+rr;
              v:=true;
            end;
          end;
        end;
    end;
  until not v;
end;

procedure TBravo.Fill(e: Integer);
begin
  ZeroBravo;
  Items[e].r:=1;
  Fill;
end;

end.
