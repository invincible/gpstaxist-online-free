//(C) 2003-2005  yusupov@gmail.com
unit city10;

interface

uses
  Classes, XMLIntf;

type
  Float = Double;

  TCoord = record
    x,y: Float;
  end;

  TRef = class
  private
    fstr, fstr0, fstr1, fstr2: string;
    procedure SetStr(value: string);
  public
    property str: string read fstr write SetStr;
    property str0: string read fstr0;
    property str1: string read fstr1;
    property str2: string read fstr2;
    function Ident(str: string; var identto: string): Integer;  overload;
    function Ident(str: string): Boolean;  overload;
    procedure SaveToXmlNode(node: IXMLNode);
    procedure LoadFromXmlNode(node: IXMLNode);
  end;

  TVertex = class
  public
    Point: TCoord;
    constructor Create;  overload;
    constructor Create(sx,sy: Float);  overload;
    property x: Float  read Point.x  write Point.x;
    property y: Float  read Point.y  write Point.y;
    procedure SaveToXmlNode(node: IXMLNode);
    procedure LoadFromXmlNode(node: IXMLNode);
  end;

  TVertexList = class (TList)
  private
    function GetItem(index: Integer): TVertex;
    procedure SetItem(index: Integer; value: TVertex);
  public
    destructor Destroy;  override;
    property Items[index: Integer]: TVertex  read GetItem write SetItem;  default;
    function Len: Float;
    function Add: TVertex;  overload;
    function Add(v: TVertex): Integer;  overload;
    function Add(sx,sy: Float): TVertex;  overload;
    procedure Rebreak(maxlen: Integer);
    procedure Clear;  override;
    procedure Delete(v: TVertex);  overload;
    procedure Delete(i: Integer);  overload;
    function NearestVertex(ax, ay: Float; var r2: Float): TVertex;
    procedure SaveToXmlNode(node: IXMLNode);
    procedure LoadFromXmlNode(node: IXMLNode);
  end;

  THome = class (TVertex)
  private
    fNo: string;
    fNum: Integer;
    fNumOdd: Boolean;
    procedure SetNo(value: string);
  public
    property No: string read fNo write SetNo;
    property Num: Integer read fNum;
    property NumOdd: Boolean read fNumOdd;
    procedure SaveToXmlNode(node: IXMLNode);
    procedure LoadFromXmlNode(node: IXMLNode);
  end;

  THomeList = class (TList)
  private
    function GetItem(index: Integer): THome;
    procedure SetItem(index: Integer; value: THome);
  public
    destructor Destroy;  override;
    property Items[index: Integer]: THome  read GetItem write SetItem;  default;
    function Add: THome;  overload;
    function Add(No: string): THome;  overload;
    function Add(home: THome): Integer;  overload;
    function Identify(No: string): TCoord;
    procedure Delete(i: Integer);  overload;
    procedure Delete(h: THome);  overload;
    procedure Clear;  override;
    procedure SaveToXmlNode(node: IXMLNode);
    procedure LoadFromXmlNode(node: IXMLNode);
  end;

  TSpot = class (TRef)
  public
    Origin: string;
    Meters: Integer;
    procedure SaveToXmlNode(node: IXMLNode);
    procedure LoadFromXmlNode(node: IXMLNode);
  end;

  TLine = class (TRef)
  public
    Homes: THomeList;
    Vertexes: TVertexList;
    constructor Create;
    destructor Destroy;  override;
    function GetHome(Adr: string): string;
    function Identify(Adr: string): TCoord;
    function Len: Float;
    procedure SaveToXmlNode(node: IXMLNode);
    procedure LoadFromXmlNode(node: IXMLNode);
  end;

  TCityList = class (TList)
  private
    function GetItem(index: Integer): TRef;
    procedure SetItem(index: Integer; value: TRef);
  public
    destructor Destroy;  override;
    property Items[index: Integer]: TRef read GetItem write SetItem;  default;
    function Identify(Adr: string): TRef;
    procedure Delete(index: Integer);
    function Add(ref: TRef): Integer;
    procedure Clear;  override;
    procedure SaveToXmlNode(node: IXMLNode);
    procedure LoadFromXmlNode(node: IXMLNode);
  end;

  TCity = class
  private
  public
    City: string;
    List: TCityList;
    procedure Clear;
    procedure SaveToXmlNode(node: IXMLNode);
    procedure LoadFromXmlNode(node: IXMLNode);
    procedure Load(filename: string);
    procedure Save(filename: string);
    constructor Create;
    destructor Destroy;  override;
  end;

function TrimNo(str: string): Integer;

const
  nil_coord = -30000;

implementation

uses
  SysUtils, XMLDoc;

//---------- TRef ----------

procedure TRef.SetStr(value: string);

  function CutStreet(a: string): string;
  const
    micro: array [0..11] of string = (
      'ул.' , 'улица',
      'пер.', 'переулок',
      'пр.' , 'проспект',
      'ост.', 'остановка',
      'б-р' , 'бульвар',
      'п-д' , 'проезд'
    );
  var
    b: string;
    i: Integer;
  begin
    b:=AnsiLowerCase(a);
    for i:=Low(micro) to High(micro) do
      if (Copy(b, 1, Length(micro[i])+1)=micro[i]+' ') then
        b:=Copy(b, Length(micro[i])+1, Length(b));
    Result:=Trim(b);
  end;

var
  j: Integer;
begin
  fstr:=value;
  fstr0:=AnsiLowerCase(str);
  fstr1:=CutStreet(str0);
  j:=Pos(' ', str1);
  if j<>0 then
    fstr2:=Copy(str1, j+1, Length(str1)-j)
  else
    fstr2:='';
end;

function RefMatch(ref, s: string): Boolean;
begin
  Result:=false;
  if (Length(ref)<>0) and (Length(s)<>0) then begin
    if Copy(s, 1, Length(ref))=ref then
      if Length(s)>Length(ref) then
        Result:=(s[Length(ref)+1]=#32)
      else
        Result:=true;
  end;
end;

function TRef.Ident(str: string; var identto: string): Integer;
begin
  if RefMatch(str0, str) then begin
    Result:=3;
    identto:=str0;
  end else
    if RefMatch(str1, str) then begin
      Result:=2;
      identto:=str1;
    end else
      if RefMatch(str2, str) then begin
        Result:=1;
        identto:=str2;
      end else
        Result:=0;
end;

function TRef.Ident(str: string): Boolean;
begin
  if RefMatch(str0, str) then
    Result:=true
  else
    if RefMatch(str1, str) then
      Result:=true
    else
      if RefMatch(str2, str) then
        Result:=true
      else
        Result:=false;
end;

procedure TRef.SaveToXmlNode(node: IXMLNode);
begin
  node.AddChild('str').Text:=str;
end;

procedure TRef.LoadFromXmlNode(node: IXMLNode);
begin
  str:=node.ChildNodes['str'].Text;
end;

procedure TSpot.SaveToXmlNode(node: IXMLNode);
begin
  inherited SaveToXmlNode(node);
end;

procedure TSpot.LoadFromXmlNode(node: IXMLNode);
begin
  inherited LoadFromXmlNode(node);
end;

//---------- TVertex ----------

constructor TVertex.Create;
begin
  inherited;
  x:=nil_coord;
end;

constructor TVertex.Create(sx,sy: Float);
begin
  inherited Create;
  x:=sx;  y:=sy;
end;

procedure TVertex.SaveToXmlNode(node: IXMLNode);
begin
  node.AddChild('x').Text:=FloatToStr(x);
  node.AddChild('y').Text:=FloatToStr(y);
end;

procedure TVertex.LoadFromXmlNode(node: IXMLNode);
begin
  x:=StrToFloat(node.ChildNodes['x'].Text);
  y:=StrToFloat(node.ChildNodes['y'].Text);
end;

//---------- TVertexList ----------

destructor TVertexList.Destroy;
begin
  Clear;
  inherited Destroy;
end;

function TVertexList.GetItem(index: Integer): TVertex;
begin
  Result:=inherited Items[index];
end;

procedure TVertexList.SetItem(index: Integer; value: TVertex);
begin
  inherited Items[index]:=value;
end;

function TVertexList.Len: Float;
var
  i: Integer;
begin
  Result:=0;
  for i:=1 to Count-1 do
    Result:=Result+Sqrt(Sqr(Items[i].x-Items[i-1].x)+Sqr(Items[i].y-Items[i-1].y));
end;

function TVertexList.Add(sx,sy: Float): TVertex;
begin
  Result:=TVertex.Create(sx, sy);
  Add(Result);
end;

function TVertexList.Add: TVertex;
begin
  Result:=TVertex.Create;
  Add(Result);
end;

function TVertexList.Add(v: TVertex): Integer;
begin
  Result:=inherited Add(v);
end;

procedure TVertexList.Clear;
var
  i: Integer;
begin
  for i:=0 to Count-1 do
    Items[i].Free;
  inherited;
end;

procedure TVertexList.Delete(v: TVertex);
begin
  Delete(IndexOf(v));
end;

procedure TVertexList.Delete(i: Integer);
begin
  Items[i].Free;
  inherited Delete(i);
end;

procedure TVertexList.Rebreak(maxlen: Integer);
var
  i: Integer;
begin
  i:=1;
  while i<Count-1 do
    if maxlen<Abs(Items[i].x-Items[i-1].x)+Abs(Items[i].y-Items[i-1].y) then
      Insert(i, TVertex.Create((Items[i].x+Items[i-1].x)/2, (Items[i].y+Items[i-1].y)/2))
    else
      Inc(i);
end;

function TVertexList.NearestVertex(ax, ay: Float; var r2: Float): TVertex;
var
  i: Integer;
  r: Float;
begin
  Result:=nil;
  for i:=0 to Count-1 do begin
    r:=Sqr(ax-Items[i].x)+Sqr(ay-Items[i].y);
    if (r<r2) or (r2=-1) then begin
      Result:=Items[i];
      r2:=r;
    end;
  end;
end;

procedure TVertexList.SaveToXmlNode(node: IXMLNode);
var
  i: Integer;
begin
  for i:=0 to Count-1 do
    Items[i].SaveToXMLNode(node.AddChild('CityVertex'));
end;

procedure TVertexList.LoadFromXmlNode(node: IXMLNode);
var
  i: Integer;
begin
  for i:=0 to node.ChildNodes.Count-1 do
    Add(nil_coord, 0).LoadFromXmlNode(node.ChildNodes[i]);
end;

//---------- THome ----------

function TrimNo(str: string): Integer;
var
  s,r: string;
  i: Integer;
begin
  s:=Trim(str);
  i:=1;
  r:='';
  while i<=Length(s) do
    if (s[i] in ['1'..'9', '0']) then begin
      r:=r+s[i];
      Inc(i);
    end else
      Break;
  if Length(r)=0 then
    Result:=-1
  else
    try
      Result:=StrToInt(r)
    except
      Result:=-1;
    end;
end;

procedure THome.SetNo(value: string);
begin
  fNo:=value;
  fNum:=TrimNo(fNo);
  if Num<>-1 then
    fNumOdd:=Odd(Num);
end;

procedure THome.SaveToXmlNode(node: IXMLNode);
begin
  inherited;
  node.AddChild('no').Text:=No;
end;

procedure THome.LoadFromXmlNode(node: IXMLNode);
begin
  inherited;
  No:=node.ChildNodes['no'].Text;
end;

//---------- THomeList ----------

destructor THomeList.Destroy;
begin
  Clear;
  inherited Destroy;
end;

function THomeList.Identify(No: string): TCoord;

  function NearestNums(n: Integer; var l: Integer; var h: Integer): Boolean;
  var
    nn: Integer;
    i: Integer;
    flo, fhi: Integer;
  begin
    flo:=0;  fhi:=10000;
    l:=-1;  h:=-1;
    for i:=0 to Count-1 do begin
      nn:=Items[i].Num;
      if nn<>-1 then
        if Odd(nn)=Odd(n) then begin
          if (nn>=n) and (nn<fhi) then begin
            fhi:=nn;  h:=i;
          end;
          if (nn<=n) and (nn>flo) then begin
            flo:=nn;  l:=i;
          end;
        end;
    end;
    if (l=-1) and (h<>-1) then  l:=h;
    if (h=-1) and (l<>-1) then  h:=l;
    Result:=((l<>-1) or (h<>-1));
  end;

var
  fail: Boolean;
  n: Integer;
  a,b: Integer;
  ta,tb: THome;
begin
  Result.x:=nil_coord;
  fail:=false;
  if Count>0 then begin
    n:=TrimNo(No);
    if n=-1 then
      fail:=true
    else
      if NearestNums(n, a, b) then begin
        ta:=Items[a];
        tb:=Items[b];
        if (a=b) or (ta.Num=tb.Num) then begin
          Result.x:=ta.x;
          Result.y:=ta.y;
        end else begin
          Result.x:=(n-tb.Num)*(ta.x-tb.x)/(ta.Num-tb.Num)+tb.x;
          Result.y:=(n-tb.Num)*(ta.y-tb.y)/(ta.Num-tb.Num)+tb.y;
        end;
      end;
  end;
  if fail then
    Result.x:=nil_coord;
end;

function THomeList.Add(No: string): THome;
begin
  Result:=Add;
  Result.No:=No;
end;

function THomeList.Add(home: THome): Integer;
begin
  Result:=inherited Add(home);
end;

function THomeList.Add: THome;
begin
  Result:=THome.Create;
  Add(Result);
end;

function THomeList.GetItem(index: Integer): THome;
begin
  Result:=inherited Items[index];
end;

procedure THomeList.SetItem(index: Integer; value: THome);
begin
  inherited Items[index]:=value;
end;

procedure THomeList.Clear;
var
  i: Integer;
begin
  for i:=Count-1 downto 0 do
    Items[i].Free;
  inherited;
end;

procedure THomeList.Delete(i: Integer);
begin
  Items[i].Free;
  inherited Delete(i);
end;

procedure THomeList.Delete(h: THome);
begin
  Delete(IndexOf(h));
end;

procedure THomeList.SaveToXmlNode(node: IXMLNode);
var
  i: Integer;
begin
  for i:=0 to Count-1 do
    Items[i].SaveToXMLNode(node.AddChild('CityHome'));
end;

procedure THomeList.LoadFromXmlNode(node: IXMLNode);
var
  i: Integer;
begin
  for i:=0 to node.ChildNodes.Count-1 do
    Add.LoadFromXmlNode(node.ChildNodes[i]);
end;

//---------- TLine ----------

constructor TLine.Create;
begin
  inherited Create;
  Vertexes:=TVertexList.Create;
  Homes:=THomeList.Create;
end;

destructor TLine.Destroy;
begin
  Homes.Free;
  Vertexes.Free;
  inherited Destroy;
end;

function TLine.Len: Float;
begin
  Result:=Vertexes.Len;
end;

function TLine.GetHome(Adr: string): string;
var
  s: string;
  l: string;
begin
  s:=AnsiLowerCase(Trim(Adr));
  if Ident(s, l)=0 then
    Result:=''
  else
    if l=s then
      Result:=''
    else
      Result:=Copy(s, Length(l)+1, Length(s));
end;

function TLine.Identify(Adr: string): TCoord;

  function GetMidCoord: TCoord;
  begin
    if Vertexes.Count<>0 then begin
      Result.x:=(Vertexes[Vertexes.Count-1].x+Vertexes[0].x)/2;
      Result.y:=(Vertexes[Vertexes.Count-1].y+Vertexes[0].y)/2;
    end else
      Result.x:=nil_coord;
  end;

var
  s: string;
begin
  s:=GetHome(Adr);
  if s='' then
    Result:=GetMidCoord
  else begin
    Result:=Homes.Identify(s);
    if Result.x=nil_coord then
      Result:=GetMidCoord;
  end;
end;

procedure TLine.SaveToXmlNode(node: IXMLNode);
begin
  inherited SaveToXmlNode(node);
  Homes.SaveToXmlNode(node.AddChild('Home'));
  Vertexes.SaveToXmlNode(node.AddChild('Vertex'));
end;

procedure TLine.LoadFromXmlNode(node: IXMLNode);
begin
  inherited LoadFromXmlNode(node);
  Homes.LoadFromXmlNode(node.ChildNodes['Home']);
  Vertexes.LoadFromXmlNode(node.ChildNodes['Vertex']);
end;

//---------- TCityList ----------

destructor TCityList.Destroy;
begin
  Clear;
  inherited Destroy;
end;

function TCityList.GetItem(index: Integer): TRef;
begin
  Result:=inherited Items[index];
end;

procedure TCityList.SetItem(index: Integer; value: TRef);
begin
  inherited Items[index]:=value;
end;

function TCityList.Identify(Adr: string): TRef;
var
  i,j: Integer;
  s: string;
  l: string;
  r: TRef;
  m: Integer;
begin
  s:=AnsiLowerCase(Trim(Adr));
  r:=nil;
  m:=0;
  for i:=0 to Count-1 do begin
    j:=Items[i].Ident(s, l);
    if j<>0 then
      if j*1000+Length(l)*4>m then begin
        m:=j*1000+Length(l)*4;
        r:=Items[i];
        if (r is TSpot) then  m:=m+2;
      end;
  end;
  Result:=r;
end;

procedure TCityList.Delete(index: Integer);
begin
  if (Items[index] is TSpot) then
    (Items[index] as TSpot).Free
  else
    if (Items[index] is TLine) then
      (Items[index] as TLine).Free;
  inherited Delete(index);
end;

procedure TCityList.Clear;
var
  i: Integer;
begin
  for i:=Count-1 downto 0 do
    Delete(i);
  inherited Clear;
end;

function TCityList.Add(ref: TRef): Integer;
begin
  Result:=inherited Add(ref);
end;

procedure TCityList.LoadFromXmlNode(node: IXMLNode);
var
  i: Integer;
  l: TLine;
begin
  for i:=0 to node.ChildNodes.Count-1 do begin
    l:=TLine.Create;
    Add(l);
    l.LoadFromXmlNode(node.ChildNodes[i]);
  end;
end;

procedure TCityList.SaveToXmlNode(node: IXMLNode);
var
  i: Integer;
begin
  for i:=0 to Count-1 do
    if (Items[i] is TLine) then
      (Items[i] as TLine).SaveToXmlNode(node.AddChild('CityLine'))
end;

//---------- TCity ----------

procedure TCity.Clear;
begin
  City:='';
  List.Clear;
end;

procedure TCity.SaveToXmlNode(node: IXMLNode);
begin
  node.SetAttributeNS('city', '', city);
  List.SaveToXmlNode(node.AddChild('Lines'));
end;

procedure TCity.LoadFromXmlNode(node: IXMLNode);
begin
  city:=node.GetAttributeNS('city', '');
  List.LoadFromXmlNode(node.ChildNodes['Lines']);
end;

procedure TCity.Load(filename: string);
var
  xml: IXMLDocument;
begin
  xml:=LoadXMLDocument(filename);
  LoadFromXMLNode(xml.DocumentElement);
end;

procedure TCity.Save(filename: string);
var
  xml: IXMLDocument;
begin
  xml:=NewXMLDocument;
  xml.Options:=[doNodeAutoIndent];
  xml.Encoding:='unicode';
  xml.DocumentElement:=xml.AddChild('City');
  SaveToXmlNode(xml.DocumentElement);
  xml.SaveToFile(filename);
end;

constructor TCity.Create;
begin
  inherited Create;
  List:=TCityList.Create;
end;

destructor TCity.Destroy;
begin
  List.Destroy;
  inherited Destroy;
end;

end.
