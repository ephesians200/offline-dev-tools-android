package com.codingtools.app.tools

private val R = Category.REFERENCE
private fun lang(id: String, title: String, text: String) = ref("cheat-$id", "$title Cheat Sheet", R, "Syntax, idioms and gotchas – works offline", text, kw = "$id syntax reference language")

private val ascii = (0..127).joinToString("\n") { c -> val name = when (c) { 0 -> "NUL"; 7 -> "BEL"; 8 -> "BS"; 9 -> "TAB"; 10 -> "LF"; 13 -> "CR"; 27 -> "ESC"; 32 -> "SPACE"; 127 -> "DEL"; else -> if (c < 32) "ctrl" else c.toChar().toString() }; "%3d  0x%02X  %s  %s".format(c, c, Integer.toBinaryString(c).padStart(8, '0'), name) }

val referenceTools: List<Tool> = listOf(
    lang("python", "Python", """
Types      int float complex str bool None | list [] tuple () dict {} set {}
Strings    f"{x:>8.2f}"  s[::-1]  s.split(",")  ",".join(xs)  s.strip()
Lists      [x*2 for x in xs if x>0]  xs.sort(key=len)  sorted(xs, reverse=True)
Dicts      d.get(k, 0)  {k: v for k, v in pairs}  d | other  for k, v in d.items()
Unpack     a, *rest = xs   first, second = pair   def f(*args, **kw)
Control    if/elif/else · for x in range(n) · while · match x: case 1: ...
Functions  def f(a, b=1, *, c): ...   lambda x: x+1   @decorator
Classes    class A(B): def __init__(self): ...  @dataclass  @property  @staticmethod
Errors     try: ... except ValueError as e: ... finally: ...  raise X("msg")
Files      with open(p, encoding="utf-8") as f: f.read()   pathlib.Path(p).read_text()
Modules    import os, sys, json, re, itertools, collections, functools, asyncio
Async      async def f(): await g()   asyncio.run(main())   asyncio.gather(*tasks)
Tools      python -m venv .venv · pip install -r requirements.txt · pytest · ruff · mypy
Gotchas    mutable default args · is vs == · late-binding closures · GIL for CPU threads
"""),
    lang("javascript", "JavaScript / TypeScript", """
Decl       const x = 1;  let y = 2;  (avoid var)
Types(TS)  string number boolean null undefined unknown any never | T[] Array<T> | {a:number}
Strings    `Hello ${'$'}{name}`  s.split(",")  s.includes("x")  s.padStart(5,"0")
Arrays     map filter reduce find some every flat flatMap  [...a, ...b]  a.at(-1)
Objects    {a, b}  {...o, x:1}  const {a, b: c = 5} = o  Object.entries(o)  o?.x ?? "default"
Functions  const f = (a, b = 1) => a + b;   function g(...rest) {}
Classes    class A extends B { #priv = 1; static s = 0; constructor() { super(); } get x() {} }
Async      async/await · Promise.all([...]) · Promise.allSettled · fetch(url).then(r => r.json())
Modules    import x, { y } from "./m.js";   export default f;   dynamic import()
TS types   interface A {x: number}  type U = "a" | "b"  Partial<T> Pick<T,K> Record<K,V> ReturnType<F>
Generics   function id<T>(x: T): T   <T extends {id: string}>
DOM        document.querySelector(".x")  el.addEventListener("click", fn)  localStorage.setItem
Gotchas    == vs ===  this binding  floating point  typeof null  NaN !== NaN  array holes
Tooling    npm i · npx tsc --noEmit · eslint · prettier · vitest/jest
"""),
    lang("java", "Java", """
Types      int long double boolean char byte | String | List<T> Map<K,V> Set<T> Optional<T>
Strings    String.format("%5.2f", x)  s.split(",")  String.join(",", xs)  "a".repeat(3)  text blocks triple-quotes
Collections new ArrayList<>()  List.of(1,2)  Map.of("a",1)  Collections.sort(xs)
Streams    xs.stream().filter(x -> x>0).map(String::valueOf).collect(Collectors.toList())
Classes    public class A extends B implements I { private final int x; public A(int x){this.x=x;} }
Records    public record Point(int x, int y) {}      sealed interface S permits A, B {}
Enums      enum Color { RED, GREEN; }   switch (c) { case RED -> ...; default -> ...; }
Generics   <T extends Comparable<T>> T max(T a, T b)
Errors     try (var r = open()) { ... } catch (IOException | RuntimeException e) { } finally { }
Concurrency ExecutorService · CompletableFuture · synchronized · AtomicInteger · virtual threads (21)
IO         Files.readString(Path.of(p))   Files.write(...)   BufferedReader
Build      mvn clean verify · gradle build · java -jar app.jar
Gotchas    == vs equals · integer cache · checked exceptions · type erasure · null
"""),
    lang("c", "C", """
Types      char short int long long long float double | unsigned | size_t | stdint.h int32_t uint8_t
Pointers   int *p = &x;  *p = 5;  p[i]  p + 1  void *  const char *s  int **pp
Arrays     int a[5] = {0};  sizeof(a)/sizeof(a[0])  (decays to pointer in functions)
Strings    char s[] = "hi";  strlen strcpy strncpy strcmp strcat snprintf  (NUL-terminated!)
Memory     malloc(n*sizeof *p)  calloc  realloc  free(p);  always check for NULL
Structs    typedef struct { int x; double y; } T;  T t = {.x=1};  t.x  p->x
Control    if/else  switch  for(;;)  while  do{}while  goto
Preproc    #include <stdio.h>  #define MAX(a,b) ((a)>(b)?(a):(b))  #ifdef  #pragma once
IO         printf("%d %s %f\n")  scanf  fgets(buf, n, stdin)  fopen fread fwrite fclose
Build      gcc -Wall -Wextra -O2 -g main.c -o app   make   cmake -B build
Gotchas    buffer overflow · use-after-free · integer overflow · uninitialised vars · UB
"""),
    lang("cpp", "C++", """
Modern     auto x = 1;  nullptr  enum class  constexpr  [[nodiscard]]  structured bindings auto [a,b]=p;
Strings    std::string s;  s.substr(i,n)  std::string_view  std::format("{:>8.2f}", x) (C++20)
Containers vector deque list map unordered_map set array  v.push_back(x)  v.emplace_back()
Algorithms std::sort(v.begin(), v.end())  find  transform  accumulate  ranges (C++20)
Smart ptrs std::unique_ptr<T>  std::make_unique<T>()  std::shared_ptr<T>  weak_ptr
Classes    class A : public B { public: A(int x): x_(x) {} virtual ~A() = default; private: int x_; };
Templates  template<typename T> T max(T a, T b);   concepts: template<std::integral T>
Lambdas    [&](int x){ return x*2; }   [=] capture by value
RAII       resources are released in destructors – avoid raw new/delete
Move       std::move  T(T&&)  rule of 0/3/5
Threads    std::thread · std::mutex · std::lock_guard · std::async · std::atomic
Build      g++ -std=c++20 -Wall -O2 main.cpp   cmake · clang-tidy · sanitizers -fsanitize=address
"""),
    lang("csharp", "C# / .NET", """
Types      int long double decimal bool char string object var | int? nullable | record
Strings    ${'$'}"Hi {name,10}"  s.Split(',')  string.Join(",", xs)  @"verbatim"  raw triple-quotes...triple-quotes
Collections List<T> Dictionary<K,V> HashSet<T> Queue<T>  new[] {1,2}  [1,2,3] (C# 12)
LINQ       xs.Where(x => x > 0).Select(x => x * 2).OrderBy(x => x).ToList()
Classes    public class A : B, IFoo { public int X { get; set; } public A(int x) => X = x; }
Records    public record Person(string Name, int Age);   with-expressions: p with { Age = 30 }
Pattern    x switch { > 0 => "pos", 0 => "zero", _ => "neg" }   if (o is Foo f) {...}
Async      async Task<T> F() { await Task.Delay(1); }   await Task.WhenAll(a, b)
Errors     try { } catch (Exception e) when (e is IOException) { } finally { }  using var r = ...;
Null       ?.  ??  ??=  !  (nullable reference types)
CLI        dotnet new console · dotnet run · dotnet test · dotnet publish -c Release
Gotchas    struct vs class copies · async void · closures over loop vars · DateTime kinds
"""),
    lang("go", "Go", """
Vars       x := 1   var y int = 2   const Pi = 3.14
Types      int int64 uint8 float64 string bool byte rune | []T map[K]V struct interface
Slices     s := []int{1,2}  s = append(s, 3)  s[1:3]  len cap  make([]int, n)  copy(dst, src)
Maps       m := map[string]int{}  v, ok := m["k"]  delete(m, "k")  for k, v := range m
Funcs      func add(a, b int) (int, error) { return a+b, nil }   variadic ...int  closures
Structs    type P struct { Name string `json:"name"` }   func (p *P) Hello() {}
Interfaces type S interface { Area() float64 }  (implicit implementation)  any
Errors     if err != nil { return fmt.Errorf("ctx: %w", err) }  errors.Is / errors.As
Defer      defer f.Close()    panic / recover (rare)
Concurrency go f()  ch := make(chan int)  ch <- 1  v := <-ch  select {}  sync.WaitGroup  context
Generics   func Map[T, U any](xs []T, f func(T) U) []U
Tooling    go mod init · go run . · go test ./... · go vet · gofmt · go build -o app
Gotchas    nil map writes panic · loop var capture (pre-1.22) · slice aliasing · nil interface
"""),
    lang("rust", "Rust", """
Vars       let x = 1;  let mut y = 2;  const N: usize = 3;  shadowing
Types      i32 u64 f64 bool char &str String | Vec<T> HashMap<K,V> Option<T> Result<T,E> [T; N]
Ownership  move by default · &T shared borrow · &mut T unique borrow · Copy types · clone()
Lifetimes  fn longest<'a>(a: &'a str, b: &'a str) -> &'a str
Structs    struct P { x: i32 }  impl P { fn new(x: i32) -> Self { P { x } } }  #[derive(Debug, Clone)]
Enums      enum Shape { Circle(f64), Rect { w: f64, h: f64 } }   match s { Shape::Circle(r) => ..., _ => ... }
Traits     trait Area { fn area(&self) -> f64; }  impl Area for Circle {}  fn f<T: Area>(x: &T)  dyn Trait
Errors     fn f() -> Result<T, Box<dyn Error>> { let v = g()?; Ok(v) }   unwrap_or  map_err
Iterators  v.iter().map(|x| x * 2).filter(|x| *x > 2).collect::<Vec<_>>()
Closures   |a, b| a + b    move || ...
Concurrency std::thread::spawn · mpsc channel · Arc<Mutex<T>> · async/await with tokio
Cargo      cargo new · cargo run · cargo test · cargo clippy · cargo fmt · cargo add serde
Gotchas    borrow checker fights · String vs &str · integer overflow panics in debug
"""),
    lang("kotlin", "Kotlin", """
Vars       val x = 1 (read-only)  var y = 2  const val N = 3  lateinit var  by lazy { }
Null       String?  x?.length  x ?: "default"  x!!  x?.let { }  requireNotNull
Strings    "Hi ${'$'}name ${'$'}{x + 1}"  ""${'"'}raw""${'"'}  s.split(",")  xs.joinToString(", ")
Collections listOf mutableListOf mapOf setOf  xs.map{}.filter{}.groupBy{}.sortedBy{}  associateBy
Functions  fun add(a: Int, b: Int = 0): Int = a + b   vararg   inline  infix  operator  ext: fun String.shout()
Classes    class A(val x: Int) : B()  data class P(val a: Int)  object Singleton  companion object { }
Sealed     sealed interface Result { data class Ok(val v: Int): Result; object Err: Result }
When       when (x) { 1 -> "a"  in 2..5 -> "b"  is String -> "c"  else -> "d" }
Scope fns  let run with apply also
Coroutines suspend fun f()  launch { }  async { }.await()  withContext(Dispatchers.IO)  Flow  StateFlow
Compose    @Composable fun Hi() { Text("Hi") }  remember { mutableStateOf(0) }  LazyColumn  Modifier
Gotchas    platform types · == vs === · List vs MutableList · leaking coroutine scopes
"""),
    lang("swift", "Swift", """
Vars       let x = 1  var y = 2   type inference  String Int Double Bool Character
Optionals  var a: Int?  a ?? 0  if let v = a {}  guard let v = a else { return }  a!  a?.b
Collections [Int]  [String: Int]  Set<Int>   xs.map { ${'$'}0 * 2 }.filter { ${'$'}0 > 2 }  sorted(by: <)
Functions  func add(_ a: Int, to b: Int = 0) -> Int { a + b }   closures { (x: Int) in x + 1 }
Structs    struct P { var x: Int }   value semantics   mutating func
Classes    class A: B { init() { super.init() } deinit {} }   reference semantics   final
Enums      enum E { case a, b(Int) }   switch e { case .a: ... case .b(let n): ... }
Protocols  protocol Drawable { func draw() }  extension Drawable { }  some View  any P
Errors     enum MyErr: Error {}  func f() throws {}  do { try f() } catch { }
Concurrency async/await  Task { }  actor  @MainActor  AsyncStream
SwiftUI    struct V: View { @State var n = 0; var body: some View { Text("Hi") } }
Gotchas    retain cycles [weak self] · force unwrap crashes · value vs reference copies
"""),
    lang("php", "PHP", """
Vars       §x = 1;  §arr = [1, 2];  §map = ['a' => 1];  const X = 1;  null coalescing §a ?? 'd'
Strings    "Hi §name {§obj->x}"  'single'  strlen str_contains str_replace explode implode sprintf
Arrays     array_map array_filter array_reduce array_merge in_array array_keys usort  [...§a, ...§b]
Functions  function add(int §a, int §b = 0): int { return §a + §b; }   fn(§x) => §x * 2
Classes    class A extends B implements I { public function __construct(private int §x) {} }
Modern     enum Suit: string { case H = 'h'; }  readonly properties  match(§x) { 1 => 'a', default => 'b' }
Types      ?string  int|string  mixed  never  declare(strict_types=1);
Errors     try { } catch (Exception §e) { } finally { }  throw new RuntimeException('x');
DB         §pdo = new PDO(§dsn, §u, §p);  §st = §pdo->prepare('SELECT * FROM t WHERE id = ?');  §st->execute([§id]);
Web        §_GET §_POST §_SERVER  header('Location: /')  json_encode  json_decode  password_hash
Tools      composer require x/y · php -S localhost:8000 · phpunit · phpstan
Gotchas    loose == · always use prepared statements · output escaping htmlspecialchars
"""),
    lang("ruby", "Ruby", """
Vars       x = 1  @ivar  @@cvar  ${'$'}global  CONST  :symbol  nil
Strings    "Hi #{name}"  'raw'  s.split(",")  xs.join(", ")  s.gsub(/a/, "b")  <<~HEREDOC
Arrays     xs.map { |x| x * 2 }.select(&:even?)  xs.each_with_index  xs.sort_by { |x| -x }  xs.sum
Hashes     h = { a: 1, "b" => 2 }  h[:a]  h.fetch(:z, 0)  h.each { |k, v| }  h.transform_values
Methods    def add(a, b = 0, *rest, key: 1, &blk) a + b end   implicit return
Classes    class A < B; attr_accessor :x; def initialize(x) = @x = x; end   module Mixin; include Mixin
Blocks     [1,2].each do |x| puts x end   yield   proc  lambda ->(x) { x + 1 }
Control    if/elsif/else  unless  case x when 1 then ...  while  loop do  x if cond
Errors     begin ... rescue StandardError => e ... ensure ... end   raise ArgumentError, "msg"
Meta       define_method  method_missing  send  respond_to?  Struct.new(:a, :b)  Data.define
Tools      bundle install · rake · rspec · rubocop · irb
Gotchas    everything is an object · mutation with ! methods · frozen strings · nil.to_a
"""),
    lang("dart", "Dart / Flutter", """
Vars       var x = 1;  final y = 2;  const z = 3;  late String s;  String? maybeNull
Null       x?.length  x ?? 'default'  x!  x ??= 5   sound null safety
Strings    'Hi ${'$'}name ${'$'}{x + 1}'  s.split(',')  xs.join(', ')  r'raw'  '''multi'''
Collections [1, 2]  {'a': 1}  {1, 2}  [...a, ...b]  [for (x in xs) if (x > 0) x * 2]  xs.map((x) => x * 2).toList()
Functions  int add(int a, {int b = 0, required int c}) => a + b + c;  typedef  closures
Classes    class A extends B with M implements I { final int x; A(this.x); factory A.of() => A(1); }
Records    (int, String) r = (1, 'a');   var (a, b) = r;   patterns  switch (x) { case int n when n > 0: ... }
Async      Future<int> f() async { await Future.delayed(...); }   Stream<int>  async*  yield
Flutter    class W extends StatelessWidget { Widget build(BuildContext c) => Text('Hi'); }  setState  Provider/Riverpod
Layout     Column Row Stack Expanded Padding Container ListView.builder Scaffold
Tools      dart pub add x · flutter run · flutter test · dart format · dart analyze
Gotchas    widgets rebuild often · keys · BuildContext across async gaps
"""),
    lang("r", "R", """
Vars       x <- 1   y = 2   c(1,2,3)   1:10   seq(0,1,by=0.1)   NULL NA TRUE FALSE
Types      numeric integer character logical factor | vector list matrix data.frame
Index      v[2]  v[-1]  v[v>2]  l[["a"]]  l${'$'}a  df[df${'$'}x > 1, c("a","b")]   (1-based!)
Functions  f <- function(a, b = 1) { a + b }   sapply(xs, f)   lapply   Map   |> pipe
Data       df <- read.csv("f.csv")  head(df)  str(df)  summary(df)  nrow ncol  merge  aggregate
Tidyverse  library(dplyr); df |> filter(x > 1) |> group_by(g) |> summarise(m = mean(y))
Stats      mean median sd var quantile cor lm(y ~ x, data = df)  t.test  table
Plot       plot(x, y)  hist(x)  ggplot(df, aes(x, y)) + geom_point()
Strings    paste("a", "b", sep="-")  sprintf("%.2f", x)  grepl  sub gsub  strsplit
Control    if (x) {} else {}  for (i in 1:n) {}  while  repeat  ifelse(cond, a, b)
Gotchas    1-based · vectors recycle · factors · stringsAsFactors · <- vs =
"""),
    lang("sql", "SQL (ANSI + dialect notes)", """
Select     SELECT a, COUNT(*) AS n FROM t WHERE x > 1 GROUP BY a HAVING n > 2 ORDER BY n DESC LIMIT 10;
Joins      INNER JOIN · LEFT JOIN · RIGHT JOIN · FULL OUTER JOIN · CROSS JOIN · self-join
Insert     INSERT INTO t (a,b) VALUES (1,'x'),(2,'y');   INSERT … SELECT …
Update     UPDATE t SET a = 1 WHERE id = 5;      DELETE FROM t WHERE id = 5;
DDL        CREATE TABLE t (id INTEGER PRIMARY KEY, name TEXT NOT NULL UNIQUE, ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
Indexes    CREATE INDEX idx ON t (a, b);   EXPLAIN / EXPLAIN ANALYZE
CTE        WITH x AS (SELECT …) SELECT * FROM x;    recursive CTEs
Window     ROW_NUMBER() OVER (PARTITION BY g ORDER BY ts)   SUM(x) OVER (ORDER BY ts ROWS BETWEEN 2 PRECEDING AND CURRENT ROW)
Subquery   WHERE id IN (SELECT …)   EXISTS (SELECT 1 …)
Null       IS NULL · COALESCE(a, 0) · NULLIF(a, 0) – NULL = NULL is not true
Dialects   LIMIT (MySQL/PG/SQLite) vs TOP / OFFSET-FETCH (T-SQL) vs FETCH FIRST (Oracle)
           AUTO_INCREMENT (MySQL) · SERIAL / GENERATED (PG) · AUTOINCREMENT (SQLite) · IDENTITY (T-SQL)
           string concat: || (ANSI/PG/SQLite) · CONCAT() (MySQL) · + (T-SQL)
           upsert: ON CONFLICT DO UPDATE (PG/SQLite) · ON DUPLICATE KEY UPDATE (MySQL) · MERGE (T-SQL/Oracle)
Safety     always use parameterised queries – never string-concatenate user input
"""),
    lang("bash", "Bash / Shell", """
Vars       name="x"  echo "§name"  §{name:-default}  §((1+2))  §(command)  export VAR=1
Test       [[ -f file ]]  [[ -z "§s" ]]  [[ §a == §b ]]  [[ §n -gt 5 ]]   &&  ||
Loops      for f in *.txt; do …; done   while read -r line; do …; done < file   for i in {1..5}
Functions  f() { local x="§1"; echo "§x"; return 0; }   §@ §# §? §0 §§
Pipes      cmd1 | cmd2   cmd > out   cmd >> out   cmd 2>&1   cmd < in   <(process subst)
Text       grep -rn "x" .  sed -i 's/a/b/g' f  awk -F, '{print §2}' f  cut -d, -f1  sort | uniq -c  tr a-z A-Z
Files      ls -la  cp -r  mv  rm -rf  mkdir -p  find . -name "*.kt" -mtime -1  chmod +x  ln -s
Safety     set -euo pipefail   quote "§vars"   shellcheck script.sh
Process    ps aux | grep x  kill -9 PID  jobs  bg  fg  nohup cmd &   trap 'cleanup' EXIT
Net        curl -fsSL url  wget  ssh user@host  scp  rsync -av  nc -zv host 443
Gotchas    word splitting · unquoted globs · spaces in filenames · set -e quirks
"""),
    lang("lua", "Lua", """
Vars       local x = 1   global y = 2   nil true false   // everything else is a table
Types      number string boolean table function nil thread userdata
Strings    "a" .. "b"   #s   s:upper()   string.format("%5.2f", x)   s:gsub("a", "b")   [[long string]]
Tables     t = {1, 2, 3}   t[#t+1] = 4   t.key = "v"   for i, v in ipairs(t) do … end   pairs(t)
Funcs      local function f(a, b) return a + b end   varargs ...   closures   multiple returns
OOP        local A = {}; A.__index = A; function A.new(x) return setmetatable({x=x}, A) end
Control    if x then … elseif y then … else … end   while   repeat … until   for i = 1, 10, 2 do
Errors     pcall(f, args)   error("msg")   assert(cond, "msg")
Modules    local m = require("mod")   return M
Gotchas    1-based arrays · # on tables with holes · == on tables is identity · nil in tables
"""),
    lang("scala", "Scala", """
Vars       val x = 1 (immutable)   var y = 2   lazy val   type inference
Collections List(1,2) Vector Map("a" -> 1) Set   xs.map(_ * 2).filter(_ > 2).foldLeft(0)(_ + _)
Case class case class P(name: String, age: Int)   p.copy(age = 5)   pattern matching
Match      x match { case 1 => "a"  case n if n > 5 => "b"  case Some(v) => v  case _ => "z" }
Traits     trait T { def f: Int }   class A extends B with T   sealed trait / enum (Scala 3)
Options    Option(x).map(_ + 1).getOrElse(0)   Either[L, R]   Try(...)
For        for { a <- xs; b <- ys if a < b } yield (a, b)
Implicits  given / using (Scala 3)   implicit class   extension methods
Futures    Future { … }   .map   for-comprehension   Await.result
Build      sbt compile · sbt test · scala-cli run file.scala
"""),
    lang("haskell", "Haskell", """
Basics     let x = 1   x :: Int   f :: Int -> Int -> Int   f a b = a + b   (pure, lazy, statically typed)
Lists      [1,2,3]  1 : xs  map f xs  filter p xs  foldr (+) 0 xs  zip  take 5  [x*2 | x <- xs, x > 1]
Patterns   f [] = 0; f (x:xs) = x + f xs   case m of Just v -> v; Nothing -> 0
Types      data Shape = Circle Double | Rect Double Double deriving Show   newtype  type
Classes    class Shape a where area :: a -> Double   instance Shape Circle where …
Monads     Maybe · Either · IO · do { x <- m; return x }   >>=  <*>  <$>  traverse  mapM_
Records    data P = P { name :: String, age :: Int }   p { age = 5 }
Where      f x = y * 2 where y = x + 1      guards: | x > 0 = "pos" | otherwise = "neg"
Tools      ghc · cabal · stack · ghci · hlint
"""),
    lang("perl", "Perl", """
Vars       my §s = "x";  my @a = (1,2,3);  my %h = (a => 1);  §a[0]  §h{a}  §#a  scalar(@a)
Strings    "Hi §name"  . concatenation  x repetition  length substr index sprintf join split
Regex      §s =~ /(\d+)/  §1  s/old/new/g  tr/a-z/A-Z/  m{^foo}i  named captures (?<n>…)
Control    if/elsif/else  unless  foreach my §x (@a) {}  while  last next redo
Subs       sub add { my (§a, §b) = @_; return §a + §b; }   references \@a \%h  ->[0]  ->{k}
Files      open(my §fh, '<', §path) or die "§!";  while (<§fh>) { chomp; … }  close §fh
Modules    use strict; use warnings; use List::Util qw(sum max first);  use Data::Dumper;
CLI        perl -ne 'print if /x/' f   perl -pi -e 's/a/b/g' f   cpan install X
"""),
    lang("matlab", "MATLAB / Octave", """
Basics     x = 5;  A = [1 2 3; 4 5 6];  v = 1:5;  linspace(0,1,11)  zeros(3) ones(3) eye(3) rand(3)
Index      A(2,3)  A(:,1)  A(end,:)  v(v>2)   (1-based, column-major)
Ops        A*B (matrix)  A.*B (element-wise)  A'  inv(A)  A\b  det(A)  eig(A)  rank(A)
Funcs      function y = f(x) y = x.^2; end   @(x) x.^2   arrayfun  cellfun
Control    if…elseif…else…end   for i=1:n … end   while … end   switch x case 1 … end
Plot       plot(x,y)  hold on  xlabel('x')  legend('a')  subplot(2,1,1)  figure  saveas
Strings    sprintf("%d", n)  strcat  strsplit  regexp  num2str  str2double  disp  fprintf
Data       readtable("f.csv")  struct  cell {}  containers.Map
Gotchas    1-based · ' conjugate transpose · auto-expansion · output suppression with ;
"""),

    ref("git", "Git Command Cheat Sheet", R, "Everyday and rescue commands", """
Setup      git config --global user.name "Name" · git config --global user.email "e@x.com" · git init · git clone URL
Status     git status -sb · git diff · git diff --staged · git log --oneline --graph --decorate -20
Stage      git add -p (interactive) · git add . · git restore --staged file · git rm --cached file
Commit     git commit -m "msg" · git commit --amend --no-edit · git commit --fixup SHA
Branch     git switch -c feature · git switch main · git branch -d name · git branch -vv
Merge      git merge feature · git merge --no-ff · git merge --abort
Rebase     git rebase main · git rebase -i HEAD~5 · git rebase --continue / --abort
Remote     git remote -v · git fetch --prune · git pull --rebase · git push -u origin branch · git push --force-with-lease
Undo       git restore file (discard changes) · git reset --soft HEAD~1 · git reset --hard HEAD~1 · git revert SHA
Stash      git stash push -m "wip" · git stash list · git stash pop · git stash apply stash@{1}
Tags       git tag -a v1.0 -m "msg" · git push --tags
Inspect    git blame file · git show SHA · git bisect start/good/bad · git reflog (recover lost commits!)
Cherry     git cherry-pick SHA · git cherry-pick A..B
Clean      git clean -fdn (dry run) · git clean -fd
Search     git log -S"text" · git log --grep="fix" · git grep "text"
"""),
    ref("git-recipes", "Git Recipes (How do I…?)", R, "Copy-paste fixes for common git situations", """
Undo last commit, keep changes      git reset --soft HEAD~1
Undo last commit, discard changes   git reset --hard HEAD~1
Change last commit message          git commit --amend -m "new message"
Add a forgotten file to last commit git add file && git commit --amend --no-edit
Move commits to a new branch        git branch new-branch && git reset --hard origin/main && git switch new-branch
Delete remote branch                git push origin --delete branch
Rename current branch               git branch -m new-name
Discard all local changes           git restore . && git clean -fd
Recover a deleted branch/commit     git reflog  → git switch -c rescue SHA
Remove a file from history          git filter-repo --path secret.txt --invert-paths   (rotate the secret too!)
Squash last 3 commits               git rebase -i HEAD~3   (mark 2nd/3rd as 'squash')
Sync fork with upstream             git remote add upstream URL && git fetch upstream && git rebase upstream/main
Undo a pushed commit safely         git revert SHA && git push
Stop tracking a file but keep it    git rm --cached file && echo file >> .gitignore
See what changed between branches   git diff main...feature
Find the commit that broke it       git bisect start; git bisect bad; git bisect good v1.0
Pull with rebase by default         git config --global pull.rebase true
"""),
    ref("patterns", "Design Patterns (with Kotlin sketches)", R, "Creational, structural, behavioural", """
CREATIONAL
Singleton   object Config { val url = "…" }                         one instance (use sparingly)
Factory     fun create(type: String): Shape = when(type) { "c" -> Circle() else -> Square() }
Builder     Request.Builder().url(u).header(k, v).build()           step-by-step construction (or named args)
Prototype   val copy = original.copy(name = "new")                  clone existing objects
STRUCTURAL
Adapter     class NewApi(private val old: OldApi) : Target          make incompatible interfaces work together
Decorator   class Logged(val inner: Service) : Service by inner     add behaviour by wrapping
Facade      class Billing { fun pay() { auth(); charge(); receipt() } }   simple API over a complex subsystem
Proxy       lazy / caching / access-control wrapper around an object
Composite   interface Node { fun size(): Int }  class Dir(val kids: List<Node>)   tree of uniform objects
BEHAVIOURAL
Observer    StateFlow / LiveData / callbacks                        notify subscribers on change
Strategy    val sort: (List<Int>) -> List<Int>                      swap algorithms at runtime
Command     sealed interface Cmd; undo stack                        encapsulate actions
State       sealed interface UiState { Loading; Success; Error }    behaviour changes with state
Iterator    operator fun iterator()                                 traverse without exposing internals
Template    abstract fun step()                                     skeleton in base class
Repository  interface UserRepo { suspend fun get(id: Int): User }   hide data source (DB / network)
Dependency Injection  class A(private val repo: Repo)               pass dependencies in (Hilt / Koin / manual)
PRINCIPLES  SOLID · DRY · KISS · YAGNI · composition over inheritance · law of Demeter
"""),
    ref("bigo", "Big-O & Algorithm Reference", R, "Complexities of common structures and algorithms", """
Structure        Access    Search    Insert    Delete    Notes
Array            O(1)      O(n)      O(n)      O(n)      contiguous memory
Dynamic array    O(1)      O(n)      O(1)*     O(n)      *amortised append
Linked list      O(n)      O(n)      O(1)      O(1)      given the node
Hash table       -         O(1)      O(1)      O(1)      O(n) worst case
Binary search tree O(log n) O(log n)  O(log n)  O(log n)  balanced; O(n) degenerate
Heap             O(1) top  O(n)      O(log n)  O(log n)  priority queue
Trie             -         O(k)      O(k)      O(k)      k = key length

Sorting          Best        Average     Worst       Space    Stable
Bubble           O(n)        O(n²)       O(n²)       O(1)     yes
Insertion        O(n)        O(n²)       O(n²)       O(1)     yes
Selection        O(n²)       O(n²)       O(n²)       O(1)     no
Merge            O(n log n)  O(n log n)  O(n log n)  O(n)     yes
Quick            O(n log n)  O(n log n)  O(n²)       O(log n) no
Heap             O(n log n)  O(n log n)  O(n log n)  O(1)     no
Counting/Radix   O(n+k)      O(n+k)      O(n+k)      O(n+k)   yes

Graphs           BFS / DFS O(V+E) · Dijkstra O((V+E) log V) · Bellman-Ford O(VE) · Floyd-Warshall O(V³) · Topological sort O(V+E) · Kruskal/Prim O(E log V)
Searching        linear O(n) · binary O(log n) (sorted input)
Growth order     O(1) < O(log n) < O(n) < O(n log n) < O(n²) < O(2ⁿ) < O(n!)
Master theorem   T(n)=aT(n/b)+n^d : d>log_b a → n^d · d=log_b a → n^d log n · d<log_b a → n^(log_b a)
Techniques       two pointers · sliding window · binary search on answer · prefix sums · DP · greedy · backtracking · union-find · monotonic stack
"""),
    ref("shortcuts", "IDE Shortcuts: VS Code · IntelliJ/Android Studio · Vim · Emacs", R, "Keyboard shortcut sheets", """
VS CODE (Ctrl; Cmd on Mac)
Command palette Ctrl+Shift+P · Quick open Ctrl+P · Go to symbol Ctrl+Shift+O · Go to line Ctrl+G
Multi-cursor Alt+Click / Ctrl+D (next match) · Move line Alt+↑/↓ · Duplicate Shift+Alt+↓ · Comment Ctrl+/
Rename symbol F2 · Go to definition F12 · Peek Alt+F12 · Format Shift+Alt+F · Terminal Ctrl+`

INTELLIJ / ANDROID STUDIO
Search everywhere Shift Shift · Find action Ctrl+Shift+A · Recent files Ctrl+E · Go to class Ctrl+N
Refactor menu Ctrl+Alt+Shift+T · Rename Shift+F6 · Extract function Ctrl+Alt+M · Reformat Ctrl+Alt+L
Optimise imports Ctrl+Alt+O · Quick fix Alt+Enter · Generate Alt+Insert · Run Shift+F10 · Debug Shift+F9
Select more Ctrl+W · Duplicate line Ctrl+D · Delete line Ctrl+Y · Find usages Alt+F7 · Go to implementation Ctrl+Alt+B

VIM
Modes   i insert · Esc normal · v visual · V line-visual · : command
Move    h j k l · w b e (words) · 0 ^ $ · gg G · {n}G · % (matching bracket) · Ctrl+u/d (half page)
Edit    x delete char · dd delete line · yy yank · p paste · u undo · Ctrl+r redo · . repeat · ciw change word
Search  /text · ?text · n N · :%s/old/new/gc · * (word under cursor)
Files   :w · :q · :wq · :q! · :e file · :split · :vsplit · Ctrl+w w

EMACS
C-x C-f open · C-x C-s save · C-x C-c quit · C-g cancel · C-s search · M-x command · C-space mark · C-w cut · M-w copy · C-y paste · C-/ undo
"""),
    ref("precedence", "Operator Precedence", R, "High → low, for C-family, JavaScript and Python", """
C / C++ / Java / C# (high → low)
1  () [] . ->  ++ -- (postfix)
2  ! ~ ++ -- (prefix) + - (unary) * & (unary) sizeof cast
3  * / %
4  + -
5  << >>
6  < <= > >=
7  == !=
8  &    9  ^    10  |
11 &&   12 ||
13 ?:  =  += -= …   14 ,
Common trap: x & 1 == 0 parses as x & (1 == 0) in C – use parentheses.

JavaScript extras: ?? has lower precedence than || and cannot mix without parentheses; ** is right-associative and binds tighter than unary minus on its left.

Python (high → low)
**  ·  +x -x ~x  ·  * / // % @  ·  + -  ·  << >>  ·  &  ·  ^  ·  |  ·  comparisons, in, is, not in  ·  not  ·  and  ·  or  ·  if-else  ·  lambda  ·  :=
Note: comparisons chain: 1 < x < 5.  and/or return operands, not booleans.

Kotlin: postfix · prefix · multiplicative · additive · range .. · infix fn · elvis ?: · named checks in/is · comparison · equality · && · || · assignment
"""),
    ref("format-specs", "Format Specifiers: printf · strftime · Python · Java", R, "Quick lookup", """
printf (C / Java / Kotlin / Go / PHP)
%d integer · %i · %u unsigned · %x hex · %X HEX · %o octal · %b binary(Java/Go) · %e sci · %f float · %g shortest
%s string · %c char · %p pointer · %n newline(Java) · %% literal percent
Flags   %5d width · %-5d left · %05d zero-pad · %+d sign · %.2f precision · %,d thousands (Java) · %10.3f

Python format-spec  f"{x:>10}" right · "<" left · "^" centre · "{x:08.3f}" · "{x:,}" · "{x:b}" "{x:#x}" · "{x:.1%}" · "{x!r}" repr · f"{x=}" debug

strftime (C / Python / PHP / Ruby)
%Y 2026 · %y 26 · %m 10 · %d 13 · %H 14 · %I 02 · %M 30 · %S 05 · %p PM · %A Tuesday · %a Tue · %B October · %b Oct
%j day of year · %U week · %Z zone · %z +0000 · %s epoch · %F = %Y-%m-%d · %T = %H:%M:%S

Java / Kotlin DateTimeFormatter: yyyy MM dd HH hh mm ss SSS a EEEE EEE MMMM MMM XXX (offset) z (zone)
.NET:  yyyy MM dd HH mm ss fff tt dddd MMMM zzz  · numeric: N2 C2 P1 D5 X8
Go reference time: Mon Jan 2 15:04:05 MST 2006  → "2006-01-02 15:04:05"
"""),
    ref("http-status", "HTTP Status Codes", R, "Every code you will meet", """
1xx  100 Continue · 101 Switching Protocols · 102 Processing · 103 Early Hints
2xx  200 OK · 201 Created · 202 Accepted · 203 Non-Authoritative · 204 No Content · 205 Reset Content · 206 Partial Content
3xx  300 Multiple Choices · 301 Moved Permanently · 302 Found · 303 See Other · 304 Not Modified · 307 Temporary Redirect · 308 Permanent Redirect
4xx  400 Bad Request · 401 Unauthorized (unauthenticated) · 402 Payment Required · 403 Forbidden · 404 Not Found · 405 Method Not Allowed
     406 Not Acceptable · 408 Request Timeout · 409 Conflict · 410 Gone · 411 Length Required · 412 Precondition Failed · 413 Payload Too Large
     414 URI Too Long · 415 Unsupported Media Type · 416 Range Not Satisfiable · 418 I'm a teapot · 422 Unprocessable Content · 423 Locked
     425 Too Early · 426 Upgrade Required · 428 Precondition Required · 429 Too Many Requests · 431 Header Fields Too Large · 451 Unavailable For Legal Reasons
5xx  500 Internal Server Error · 501 Not Implemented · 502 Bad Gateway · 503 Service Unavailable · 504 Gateway Timeout · 505 HTTP Version Not Supported · 507 Insufficient Storage · 511 Network Auth Required
Tips 401 = who are you? · 403 = I know you, no · 404 vs 410 · 301 cached forever (browsers) · 307/308 keep the method · 429 → read Retry-After
"""),
    ref("http-headers", "HTTP Headers & Methods", R, "Request, response, security and caching headers", """
METHODS   GET (safe, idempotent) · HEAD · POST · PUT (idempotent replace) · PATCH · DELETE (idempotent) · OPTIONS (CORS preflight) · TRACE · CONNECT
REQUEST   Accept · Accept-Encoding · Accept-Language · Authorization: Bearer <token> · Cache-Control · Content-Type · Content-Length · Cookie · Host · If-None-Match · If-Modified-Since · Origin · Range · Referer · User-Agent · X-Forwarded-For
RESPONSE  Content-Type · Content-Encoding · Content-Disposition · Content-Length · ETag · Last-Modified · Location · Retry-After · Set-Cookie · Vary · WWW-Authenticate · Link
CACHING   Cache-Control: max-age, s-maxage, no-cache, no-store, private, public, immutable, stale-while-revalidate · ETag · Expires · Age
CORS      Access-Control-Allow-Origin / -Methods / -Headers / -Credentials / -Max-Age · Access-Control-Expose-Headers
SECURITY  Strict-Transport-Security · Content-Security-Policy · X-Content-Type-Options: nosniff · X-Frame-Options · Referrer-Policy · Permissions-Policy · Cross-Origin-Opener-Policy · Cross-Origin-Resource-Policy
COOKIES   Set-Cookie: k=v; Path=/; Max-Age=3600; HttpOnly; Secure; SameSite=Lax|Strict|None
CONTENT TYPES  application/json · application/x-www-form-urlencoded · multipart/form-data · text/html · text/plain · application/octet-stream · application/xml · application/pdf · image/png
"""),
    ref("mime", "MIME Types", R, "Extension → media type", """
.html text/html · .css text/css · .js text/javascript · .json application/json · .xml application/xml · .txt text/plain · .csv text/csv · .md text/markdown
.png image/png · .jpg/.jpeg image/jpeg · .gif image/gif · .webp image/webp · .svg image/svg+xml · .ico image/x-icon · .avif image/avif
.mp3 audio/mpeg · .wav audio/wav · .ogg audio/ogg · .mp4 video/mp4 · .webm video/webm · .mov video/quicktime
.pdf application/pdf · .zip application/zip · .gz application/gzip · .tar application/x-tar · .7z application/x-7z-compressed
.doc application/msword · .docx application/vnd.openxmlformats-officedocument.wordprocessingml.document · .xlsx …spreadsheetml.sheet · .pptx …presentationml.presentation
.apk application/vnd.android.package-archive · .wasm application/wasm · .woff2 font/woff2 · .ttf font/ttf · .yaml application/yaml · .sql application/sql
"""),
    ref("ports", "Common Network Ports", R, "Well-known and developer ports", """
20/21 FTP · 22 SSH/SFTP · 23 Telnet · 25 SMTP · 53 DNS · 67/68 DHCP · 80 HTTP · 110 POP3 · 123 NTP · 143 IMAP · 161/162 SNMP · 389 LDAP · 443 HTTPS
445 SMB · 465/587 SMTP (TLS/submission) · 514 syslog · 636 LDAPS · 993 IMAPS · 995 POP3S · 1433 SQL Server · 1521 Oracle · 2049 NFS · 2375/2376 Docker
3000 Node/React dev · 3306 MySQL/MariaDB · 3389 RDP · 4200 Angular dev · 5000 Flask · 5173 Vite · 5432 PostgreSQL · 5672 RabbitMQ · 5900 VNC
6379 Redis · 6443 Kubernetes API · 8000 Django/HTTP alt · 8080 HTTP alt/proxy · 8443 HTTPS alt · 8888 Jupyter · 9000 PHP-FPM/SonarQube · 9092 Kafka
9200 Elasticsearch · 11211 Memcached · 15672 RabbitMQ UI · 27017 MongoDB · 5037 adb server · 8081 Metro bundler (React Native)
"""),
    ref("dns", "DNS Record Types", R, "What each record does", """
A      hostname → IPv4          AAAA   hostname → IPv6          CNAME  alias → canonical name (not at zone apex)
MX     mail servers + priority  TXT    arbitrary text (SPF, DKIM, DMARC, domain verification)
NS     authoritative servers    SOA    zone authority/serial/timers   PTR    IP → hostname (reverse DNS)
SRV    service host + port      CAA    which CAs may issue certificates   ALIAS/ANAME  CNAME-like at the apex (provider specific)
TTL    seconds a resolver may cache the answer – lower before migrations
SPF    v=spf1 include:_spf.google.com ~all     DMARC  _dmarc  v=DMARC1; p=quarantine; rua=mailto:x@y.com
Tools  dig example.com A · dig +short TXT example.com · nslookup · host · whois
"""),
    ref("linux", "Linux / Unix Command Reference", R, "Files, text, processes, network, disk", """
FILES     ls -lah · cd · pwd · cp -r · mv · rm -rf · mkdir -p · touch · ln -s · find / -name "x*" · du -sh * · df -h · stat · file · tree
VIEW      cat · less · head -n 20 · tail -f log · wc -l · nl · xxd · strings
TEXT      grep -rniE "pat" . · sed 's/a/b/g' · awk '{print §1}' · cut · sort -u · uniq -c · tr · diff -u a b · jq '.a' · xargs
PERMS     chmod 755 f · chmod +x f · chown user:group f · umask · sudo · id · whoami
PROCESS   ps aux · top / htop · kill PID · kill -9 · pkill name · jobs · bg · fg · nohup · systemctl status|start|stop|restart svc · journalctl -u svc -f
NETWORK   ip a · ss -tulpn · curl -I url · wget · ping · traceroute · dig · nc -zv host port · ssh -L 8080:localhost:80 host · scp · rsync -avz
ARCHIVE   tar -czf a.tgz dir · tar -xzf a.tgz · zip -r a.zip dir · unzip a.zip · gzip · xz
PACKAGES  apt update && apt install x · dnf install x · pacman -S x · snap · brew install x (macOS)
SYSTEM    uname -a · uptime · free -h · lscpu · lsblk · env · export · crontab -e · date · history · which · man
REDIRECT  > overwrite · >> append · 2>&1 · | pipe · tee · <<< here-string
"""),
    ref("powershell", "PowerShell Quick Reference", R, "Cmdlets and idioms", """
Basics   Get-Help cmd -Examples · Get-Command *name* · Get-Member · Get-Alias · Get-ChildItem (ls) · Set-Location (cd) · Get-Content (cat)
Pipeline Get-Process | Where-Object CPU -gt 10 | Sort-Object CPU -Desc | Select-Object -First 5 Name, CPU
Files    New-Item -ItemType Directory x · Copy-Item · Move-Item · Remove-Item -Recurse -Force · Test-Path · Out-File · Set-Content
Strings  "Hello §name" · -f "{0:N2}" · -split "," · -join ", " · -replace "a","b" · -match "regex" · §Matches
Vars     §x = 5 · §arr = @(1,2) · §h = @{a=1} · §env:PATH · [int]"5" · §null
Control  if () {} elseif {} else {} · foreach (§i in §xs) {} · ForEach-Object { §_ } · while · switch · try {} catch {} finally {}
Web      Invoke-RestMethod -Uri url · Invoke-WebRequest · ConvertTo-Json -Depth 5 · ConvertFrom-Json
System   Get-Service · Start-Service · Get-EventLog · Get-NetIPAddress · Test-NetConnection host -Port 443 · Get-ComputerInfo
Scripts  param([string]§Name) · function Foo { } · Set-ExecutionPolicy RemoteSigned -Scope CurrentUser
"""),
    ref("adb", "ADB & Android Command Cheat Sheet", R, "Debug your device from the terminal", """
Devices  adb devices · adb connect IP:5555 · adb pair IP:PORT · adb kill-server · adb usb · adb tcpip 5555
Install  adb install -r app.apk · adb uninstall com.pkg · adb shell pm list packages | grep x · adb shell pm clear com.pkg
Run      adb shell am start -n com.pkg/.MainActivity · adb shell am start -a android.intent.action.VIEW -d "scheme://x" · adb shell am force-stop com.pkg
Files    adb push local /sdcard/ · adb pull /sdcard/f . · adb shell ls /sdcard · adb shell run-as com.pkg cat databases/x.db
Logs     adb logcat · adb logcat -s Tag:V · adb logcat *:E · adb logcat -c · adb bugreport
Screen   adb exec-out screencap -p > s.png · adb shell screenrecord /sdcard/r.mp4 · adb shell input tap X Y · input text hello · input keyevent 4
Info     adb shell getprop ro.build.version.release · adb shell dumpsys battery · dumpsys activity · dumpsys meminfo com.pkg · adb shell wm size · wm density
Network  adb shell settings put global airplane_mode_on 1 · adb reverse tcp:8081 tcp:8081 · adb forward tcp:1234 tcp:1234
Gradle   ./gradlew assembleDebug · installDebug · test · connectedAndroidTest · lint · clean · --stacktrace · --refresh-dependencies · dependencies
Common errors  INSTALL_FAILED_UPDATE_INCOMPATIBLE → uninstall first · Duplicate class → exclude transitive dep · Manifest merger failed → check tools:replace
"""),
    ref("regex-cheat", "Regex Cheat Sheet & Pattern Library", R, "Syntax plus ready-made patterns (test them in the Regex Tester)", """
SYNTAX
.  any char · \d digit · \w word · \s space · \D \W \S negated · \b word boundary · ^ start · § end
[abc] set · [^abc] not · [a-z] range · (x) group · (?:x) non-capturing · (?<name>x) named · \1 backreference
* 0+ · + 1+ · ? 0/1 · {n} · {n,} · {n,m} · lazy: *? +? ?? · alternation a|b
Lookahead (?=x) (?!x) · lookbehind (?<=x) (?<!x) · flags: i case-insens · m multiline · s dotall · g global · x verbose
Flavour notes: JS has no possessive quantifiers or atomic groups · lookbehind needs modern engines · Python uses (?P<name>x) · Go (RE2) has no backreferences or lookarounds · .NET supports variable-length lookbehind

PATTERNS
Email        ^[\w.+-]+@[\w-]+(\.[\w-]+)+§
URL          ^https?://[\w.-]+(:\d+)?(/[^\s]*)?§
IPv4         ^((25[0-5]|2[0-4]\d|1?\d?\d)\.){3}(25[0-5]|2[0-4]\d|1?\d?\d)§
Hex colour   ^#?([0-9a-fA-F]{3}|[0-9a-fA-F]{6})§
Date ISO     ^\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\d|3[01])§
Time 24h     ^([01]\d|2[0-3]):[0-5]\d(:[0-5]\d)?§
Phone (intl) ^\+?[1-9]\d{6,14}§
UUID         ^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}§
Slug         ^[a-z0-9]+(-[a-z0-9]+)*§
Semver       ^v?(\d+)\.(\d+)\.(\d+)(-[\w.]+)?(\+[\w.]+)?§
Password     ^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}§
Whitespace trim   ^\s+|\s+§      HTML tag   <([a-z][\w-]*)\b[^>]*>(.*?)</\1>      Integer ^-?\d+§      Decimal ^-?\d+(\.\d+)?§
"""),
    ref("html-ref", "HTML Tags & Attributes", R, "Semantic structure and forms", """
Document  <!doctype html> <html lang> <head> <meta charset> <meta name="viewport"> <title> <link rel="stylesheet"> <script defer src> <body>
Semantic  <header> <nav> <main> <section> <article> <aside> <footer> <figure><figcaption> <details><summary> <dialog> <time datetime> <mark> <address>
Text      <h1>–<h6> <p> <br> <hr> <strong> <em> <code> <pre> <blockquote> <q> <small> <sub> <sup> <abbr title> <kbd>
Lists     <ul><li> <ol start reversed> <dl><dt><dd>
Links     <a href target="_blank" rel="noopener noreferrer" download> · mailto: · tel: · #anchor
Media     <img src alt width height loading="lazy" srcset sizes> <picture><source> <video controls> <audio controls> <iframe sandbox> <canvas> <svg>
Tables    <table><thead><tbody><tfoot><tr><th scope><td colspan rowspan><caption>
Forms     <form action method> <label for> <input type=text|email|password|number|date|checkbox|radio|file|range|color|search|tel|url required min max pattern placeholder autocomplete> <select><option> <textarea> <button type> <fieldset><legend>
Global attrs  id class style title hidden tabindex lang dir data-* contenteditable draggable aria-* role
Perf      preload · preconnect · defer/async · loading=lazy · fetchpriority
"""),
    ref("aria", "Accessibility (ARIA) Reference", R, "Roles, states and rules of thumb", """
FIRST RULE  Use native HTML (button, a, input, nav) before ARIA. No ARIA is better than bad ARIA.
LANDMARKS   banner · navigation · main · complementary · contentinfo · search · form · region (needs a name)
WIDGETS     button · link · checkbox · radio · switch · slider · tab / tablist / tabpanel · menu / menuitem · dialog · alertdialog · tooltip · combobox · listbox · progressbar
LABELS      aria-label · aria-labelledby · aria-describedby · alt text on images (alt="" if decorative)
STATES      aria-expanded · aria-pressed · aria-checked · aria-selected · aria-disabled · aria-hidden · aria-current · aria-invalid · aria-required · aria-busy
LIVE        aria-live="polite|assertive" · role=status · role=alert
KEYBOARD    everything focusable & operable · visible focus ring · logical tab order · Esc closes dialogs · arrow keys in composite widgets
CONTRAST    4.5:1 normal text · 3:1 large text & UI · don't rely on colour alone · touch targets ≥ 44×44 px (48dp Android)
MOBILE      Android contentDescription · TalkBack · iOS accessibilityLabel · VoiceOver · Dynamic Type / font scaling
"""),
    ref("css-ref", "CSS Selectors, Flexbox & Grid", R, "Layout and selector reference", """
SELECTORS  * · tag · .class · #id · a b (descendant) · a > b · a + b · a ~ b · [attr] [attr=v] [attr^=v] · :hover :focus :focus-visible :first-child :nth-child(2n+1) :not() :is() :where() :has() · ::before ::after ::placeholder
SPECIFICITY inline 1000 · id 100 · class/attr/pseudo-class 10 · element/pseudo-element 1 · !important beats all
BOX        box-sizing: border-box · margin · padding · border · width/min/max · aspect-ratio · overflow
FLEX       display:flex · flex-direction · justify-content (main axis) · align-items (cross axis) · flex-wrap · gap · flex: 1 1 0 · align-self · order
GRID       display:grid · grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)) · grid-template-areas · gap · place-items: center · grid-column: 1 / -1
POSITION   static · relative · absolute · fixed · sticky · inset · z-index
UNITS      px · rem · em · % · vw/vh · dvh · ch · fr · clamp(1rem, 2vw, 2rem) · min() max() calc()
VARIABLES  :root { --brand: #0b3d91 }  color: var(--brand, #000)
RESPONSIVE @media (min-width: 768px) · container queries @container · prefers-color-scheme · prefers-reduced-motion
ANIMATION  transition: all .2s ease · @keyframes · transform: translate/scale/rotate · will-change
"""),
    ref("tailwind", "Tailwind CSS Class Lookup", R, "Common utilities → CSS", """
SPACING   p-4 → padding:1rem · px-2 → 0.5rem L/R · m-auto · mt-8 → 2rem · gap-4 · space-y-2     (1 unit = 0.25rem)
SIZE      w-full 100% · w-1/2 · w-64 16rem · h-screen 100vh · min-h-0 · max-w-md 28rem · size-10
FLEX      flex · flex-col · items-center · justify-between · justify-center · flex-1 · grow · shrink-0 · flex-wrap
GRID      grid · grid-cols-3 · col-span-2 · gap-6
TEXT      text-sm .875rem · text-base 1rem · text-lg · text-2xl 1.5rem · font-bold 700 · font-medium 500 · leading-tight · tracking-wide · text-center · truncate · uppercase
COLOR     text-gray-700 · bg-blue-600 · border-red-500 · bg-white/50 (opacity) · hover:bg-blue-700 · dark:bg-gray-900
BORDER    border · border-2 · rounded 0.25rem · rounded-lg .5rem · rounded-full · ring-2 · divide-y
EFFECTS   shadow · shadow-lg · opacity-50 · blur-sm · transition · duration-200 · ease-in-out · transform · scale-105 · rotate-45
LAYOUT    block · inline-block · hidden · relative · absolute · fixed · sticky · inset-0 · z-10 · overflow-hidden · object-cover
RESPONSIVE sm: 640px · md: 768px · lg: 1024px · xl: 1280px · 2xl: 1536px  (mobile-first: md:flex)
STATES    hover: focus: active: disabled: group-hover: peer-checked: first: last: odd: even:
"""),
    ref("ascii", "ASCII Table (0–127)", R, "Decimal · hex · binary · character", ascii),
    ref("symbols", "Emoji & Unicode Symbols for Developers", R, "Arrows, math, box-drawing, check marks, common emoji", """
Arrows      ← ↑ → ↓ ↔ ↕ ⇐ ⇒ ⇔ ↩ ↪ ➜ ⟶ ⟵
Math        ± × ÷ ≠ ≈ ≤ ≥ ∞ √ ∑ ∏ ∫ ∂ ∆ π µ ∈ ∉ ∪ ∩ ⊂ ⊆ ∧ ∨ ¬ ∀ ∃ ∅ ℕ ℤ ℝ
Superscript ⁰ ¹ ² ³ ⁴ ⁵ ⁶ ⁷ ⁸ ⁹ ⁿ   Subscript ₀ ₁ ₂ ₃ ₄ ₅ ₆ ₇ ₈ ₉
Check/Cross ✔ ✘ ✓ ✗ ☐ ☑ ☒ ⚠ ⛔ ℹ ❓ ❗
Box         ─ │ ┌ ┐ └ ┘ ├ ┤ ┬ ┴ ┼ ═ ║ ╔ ╗ ╚ ╝   Blocks █ ▓ ▒ ░ ▀ ▄ ▌ ▐
Bullets     • ◦ ‣ ▪ ▫ ◆ ◇ ★ ☆ ● ○ ◉
Currency    $ € £ ¥ ₿ ₹ ₵ ₦ ¢ ₩ ₽
Dev emoji   🐛 bug · ✨ feature · 🔥 remove · 🚀 deploy · ♻️ refactor · 📝 docs · ✅ tests · 🔒 security · ⚡ perf · 🎨 style · 🚧 WIP · 📦 deps · 🔧 config · ⏪ revert
Keyboard    ⌘ ⌥ ⇧ ⌃ ⏎ ⌫ ⇥ ⎋
Zero-width  U+200B ZWSP · U+200D ZWJ · U+FEFF BOM · U+00A0 NBSP (invisible bugs!)
"""),
    ref("markdown-ref", "Markdown Syntax", R, "Headings, lists, tables, code", """
# H1   ## H2   ### H3                    **bold**  *italic*  ~~strike~~  `inline code`
- bullet   1. numbered   - [ ] task   - [x] done        > blockquote       --- horizontal rule
[link text](https://url "title")      ![alt](image.png)      <https://autolink>      footnote[^1]
```kotlin
fun main() = println("fenced code with language")
```
| Col A | Col B |
|-------|:-----:|
| left  | centre|
Line break: two trailing spaces or a blank line · Escape with backslash \* · HTML is allowed in most renderers
GitHub extras: @mention · #123 issue · emoji :rocket: · ```mermaid diagrams · alerts > [!NOTE]
"""),
    ref("docker-ref", "Docker & Kubernetes Cheat Sheet", R, "Containers from build to cluster", """
IMAGES    docker build -t name:tag . · docker images · docker pull · docker push · docker rmi · docker image prune -a · docker history
RUN       docker run -d --name web -p 8080:80 -v data:/data -e KEY=val --restart unless-stopped image · docker run -it --rm image sh
CONTAINERS docker ps -a · docker logs -f web · docker exec -it web sh · docker stop/start/restart · docker rm -f · docker stats · docker inspect · docker cp
COMPOSE   docker compose up -d --build · down -v · logs -f · ps · exec svc sh · config
CLEANUP   docker system prune -af --volumes
DOCKERFILE FROM · WORKDIR · COPY · RUN · ENV · EXPOSE · USER nonroot · ENTRYPOINT · CMD · HEALTHCHECK · multi-stage builds · .dockerignore
KUBECTL   kubectl get pods|svc|deploy|ing -A · describe pod x · logs -f x · exec -it x -- sh · apply -f f.yaml · delete -f f.yaml · rollout status|undo deploy/x
          kubectl scale deploy x --replicas=3 · port-forward svc/x 8080:80 · top pods · config use-context · get events --sort-by=.lastTimestamp · -n namespace
HELM      helm repo add · helm install rel chart -f values.yaml · helm upgrade --install · helm list · helm rollback
"""),
    ref("android-perms", "Android Permissions & Manifest Reference", R, "Common permissions and when they are runtime-requested", """
NORMAL (auto-granted): INTERNET · ACCESS_NETWORK_STATE · VIBRATE · WAKE_LOCK · BLUETOOTH (legacy) · RECEIVE_BOOT_COMPLETED · FOREGROUND_SERVICE
DANGEROUS (ask at runtime): CAMERA · RECORD_AUDIO · ACCESS_FINE_LOCATION / COARSE · ACCESS_BACKGROUND_LOCATION · READ_CONTACTS · READ_CALENDAR · POST_NOTIFICATIONS (13+)
          READ_MEDIA_IMAGES / VIDEO / AUDIO (13+) · BLUETOOTH_CONNECT / SCAN (12+) · NEARBY_WIFI_DEVICES · ACTIVITY_RECOGNITION · BODY_SENSORS · READ_PHONE_STATE · CALL_PHONE · SEND_SMS
SPECIAL: SYSTEM_ALERT_WINDOW · WRITE_SETTINGS · MANAGE_EXTERNAL_STORAGE · SCHEDULE_EXACT_ALARM · REQUEST_INSTALL_PACKAGES
Prefer: photo picker / SAF instead of storage permissions · approximate location first · explain why before asking
Manifest: <uses-permission/> · <uses-feature android:required="false"/> · <queries/> · android:exported (required for components with intent-filters, API 31+) · android:usesCleartextTraffic · networkSecurityConfig
Runtime (Compose): rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> … }
"""),
    ref("ios-keys", "iOS Info.plist Keys & Bundle IDs", R, "Privacy strings and common keys", """
CFBundleIdentifier (reverse-DNS, e.g. com.company.app) · CFBundleName · CFBundleDisplayName · CFBundleShortVersionString (1.2.3) · CFBundleVersion (build number)
PRIVACY (required text shown to the user)
NSCameraUsageDescription · NSMicrophoneUsageDescription · NSPhotoLibraryUsageDescription · NSPhotoLibraryAddUsageDescription
NSLocationWhenInUseUsageDescription · NSLocationAlwaysAndWhenInUseUsageDescription · NSContactsUsageDescription · NSCalendarsUsageDescription
NSBluetoothAlwaysUsageDescription · NSFaceIDUsageDescription · NSUserTrackingUsageDescription · NSMotionUsageDescription
OTHER UIApplicationSceneManifest · UILaunchScreen · UIBackgroundModes · CFBundleURLTypes (URL schemes) · LSApplicationQueriesSchemes · ITSAppUsesNonExemptEncryption · UISupportedInterfaceOrientations · NSAppTransportSecurity
Signing: Team ID · provisioning profile (Development / Ad Hoc / App Store) · entitlements · capabilities (Push, Sign in with Apple, Associated Domains)
"""),
    ref("screens", "Screen Sizes, Densities & Breakpoints", R, "Android dp buckets, iOS points, web breakpoints", """
ANDROID DENSITY  ldpi 0.75× 120dpi · mdpi 1× 160 · hdpi 1.5× 240 · xhdpi 2× 320 · xxhdpi 3× 480 · xxxhdpi 4× 640     px = dp × (dpi / 160)
ANDROID WINDOW CLASSES  Compact < 600dp (phones) · Medium 600–839dp (tablets, foldables) · Expanded ≥ 840dp
ANDROID TOUCH TARGET  48 × 48 dp minimum · icons 24dp · app icon adaptive 108dp (66dp safe zone)
iOS POINTS  iPhone SE 375×667 · iPhone 15 393×852 · iPhone 15 Pro Max 430×932 · iPad 10.9" 820×1180 · @2x/@3x assets · touch target 44pt
WEB BREAKPOINTS  480 phones · 768 tablets · 1024 laptops · 1280 desktops · 1536 large
ICON SIZES  Android launcher 48/72/96/144/192 px (+512 Play Store) · iOS 1024 App Store, 180/120/87/80/60/58/40 · PWA 192 & 512 · favicon 16/32/48
COMMON PHONE VIEWPORTS  360×800 · 390×844 · 412×915 · 414×896 · 430×932
"""),
    ref("material", "Material 3 & Cupertino Design Tokens", R, "Spacing, shape, type and colour roles", """
SPACING (dp)   4 · 8 · 12 · 16 · 24 · 32 · 48 · 64
SHAPE          none 0 · extra-small 4 · small 8 · medium 12 · large 16 · extra-large 28 · full (pill)
TYPE SCALE     Display L/M/S 57/45/36 · Headline L/M/S 32/28/24 · Title L/M/S 22/16/14 · Body L/M/S 16/14/12 · Label L/M/S 14/12/11 (sp)
COLOR ROLES    primary / onPrimary / primaryContainer · secondary · tertiary · error · background / onBackground · surface / onSurface / surfaceVariant · outline · inverseSurface · scrim
ELEVATION      level0 0dp · 1: 1dp · 2: 3dp · 3: 6dp · 4: 8dp · 5: 12dp (tonal elevation in M3)
MOTION         emphasized 500ms · standard 300ms · short 100–200ms · easing: emphasized, standard, decelerate, accelerate
COMPOSE        MaterialTheme.colorScheme.primary · MaterialTheme.typography.titleMedium · MaterialTheme.shapes.medium
CUPERTINO      SF Pro · 17pt body · large title 34pt · corner radius ~10–13 · system blue #007AFF · 44pt hit targets · translucent materials
"""),
    ref("owasp", "OWASP Top 10 (2021) Cheat Sheet", R, "Risk, example and mitigation", """
A01 Broken Access Control        IDOR, missing authz checks         → deny by default, server-side checks, ownership validation
A02 Cryptographic Failures       plaintext secrets, weak hashes     → TLS everywhere, Argon2/bcrypt, AES-GCM, no custom crypto
A03 Injection                    SQL, NoSQL, OS command, XSS        → parameterised queries, output encoding, input validation
A04 Insecure Design              no threat model                    → threat modelling, secure design patterns, rate limits
A05 Security Misconfiguration    default creds, verbose errors      → hardening, minimal surface, security headers, config reviews
A06 Vulnerable Components        outdated libs                      → dependency scanning, SBOM, patch cadence
A07 Identification & Auth Failures weak passwords, no MFA, session fixation → MFA, secure session handling, lockouts
A08 Software & Data Integrity    unsigned updates, insecure CI/CD   → signing, pinned dependencies, review pipelines
A09 Logging & Monitoring Failures no audit trail                    → log auth events, alerting, no secrets in logs
A10 SSRF                         server fetches attacker URLs       → allow-lists, block internal ranges, no raw redirects
MOBILE: store secrets in Keystore/Keychain · certificate pinning for sensitive APIs · disable backups for sensitive data · validate deep links · never log tokens · obfuscate release builds
"""),
    ref("payloads", "Injection Test Strings (for your own apps)", R, "Educational reference – only test systems you own or are authorised to test", """
SQL injection probes   '   ''   ' OR '1'='1   ' OR 1=1--   "; DROP TABLE x;--   1; WAITFOR DELAY '0:0:5'--        → fix with parameterised queries
XSS probes             <script>alert(1)</script>   "><img src=x onerror=alert(1)>   javascript:alert(1)   ' onmouseover='alert(1)   → fix with output encoding + CSP
Path traversal         ../../etc/passwd   ..%2f..%2f   %2e%2e/   ....//                                           → canonicalise & allow-list paths
Command injection      ; ls   | id   && whoami   `id`   §(id)                                                       → never build shell strings from input
Template injection     {{7*7}}   §{7*7}   <%= 7*7 %>                                                              → sandbox / avoid user-controlled templates
SSRF                   http://169.254.169.254/   http://localhost:8080/admin   file:///etc/passwd                 → block internal ranges
Open redirect          //evil.com   /\evil.com   https:evil.com                                                   → allow-list redirect targets
Unicode / length       very long strings · NUL byte %00 · RTL override U+202E · emoji · homoglyphs               → validate, normalise (NFC)
"""),
    ref("security-checklist", "CSP · HSTS · Security Header Checklist", R, "Web hardening checklist", """
[ ] HTTPS everywhere + Strict-Transport-Security: max-age=63072000; includeSubDomains; preload
[ ] Content-Security-Policy with default-src 'self' (avoid 'unsafe-inline' / 'unsafe-eval'; use nonces/hashes)
[ ] X-Content-Type-Options: nosniff
[ ] Clickjacking: frame-ancestors 'none' (or X-Frame-Options: DENY)
[ ] Referrer-Policy: strict-origin-when-cross-origin
[ ] Permissions-Policy: disable unused features (camera, mic, geolocation)
[ ] Cookies: HttpOnly; Secure; SameSite=Lax/Strict; short lifetimes
[ ] CORS: explicit origins, never * with credentials
[ ] CSRF tokens or SameSite + custom headers for state-changing requests
[ ] Rate limiting & lockout on auth endpoints · password hashing with Argon2id/bcrypt
[ ] Dependency audit (npm audit, pip-audit, OWASP dependency-check) · secrets out of repos · least privilege
[ ] Subresource Integrity on third-party scripts · remove server version banners
"""),
    ref("license-compat", "Open-Source License Compatibility", R, "What you can combine (not legal advice)", """
Permissive   MIT · BSD-2/3 · ISC · Apache-2.0 (adds patent grant)   → can be used in almost anything, keep notices
Weak copyleft LGPL-2.1/3.0 · MPL-2.0 · EPL-2.0                     → modifications to the library files must be shared; linking is OK
Strong copyleft GPL-2.0 / GPL-3.0                                    → derivative works must be GPL
Network copyleft AGPL-3.0                                            → also applies to software offered over a network
Compatibility highlights
 • MIT / BSD / ISC → compatible with everything
 • Apache-2.0 → compatible with GPL-3.0, NOT with GPL-2.0-only
 • GPL-2.0-only ↔ GPL-3.0 → not compatible (unless "or later")
 • LGPL → can be linked from proprietary code if users can relink/replace the library
 • AGPL → usually avoided in commercial SaaS unless you comply
 • Creative Commons (CC-BY etc.) → meant for content, not software
Always keep copyright notices & license texts; check each dependency's license in your build (e.g. gradle-license-plugin, license-checker).
"""),
    ref("asm", "Assembly Reference: x86-64 · ARM64 · RISC-V", R, "Registers, common instructions, calling conventions", """
x86-64 (Intel syntax)
Registers  rax rbx rcx rdx rsi rdi rbp rsp r8–r15 · eax ax al · rip · flags ZF CF SF OF
Instr      mov add sub imul idiv inc dec · and or xor not shl shr · cmp test · jmp je jne jl jg jle jge ja jb · call ret push pop lea · syscall · nop
SysV ABI   args: rdi rsi rdx rcx r8 r9 · return rax · callee-saved: rbx rbp r12–r15 · stack 16-byte aligned
Windows x64 args: rcx rdx r8 r9 + 32-byte shadow space

ARM64 (AArch64)
Registers  x0–x30 (w0–w30 = 32-bit) · sp · xzr zero · x30 = LR · x29 = FP · pc
Instr      mov add sub mul sdiv · and orr eor lsl lsr · cmp · b b.eq b.ne b.lt b.gt · bl ret · ldr str ldp stp · adrp · svc #0 · cbz cbnz
AAPCS64    args x0–x7 · return x0 · callee-saved x19–x28 · syscall number in x8 (Linux)

RISC-V (RV64)
Registers  x0 zero · x1 ra · x2 sp · a0–a7 (args/return) · t0–t6 temps · s0–s11 saved
Instr      addi add sub mul div · and or xor sll srl sra · lw sw ld sd lb sb · beq bne blt bge · jal jalr · ecall · lui auipc · li (pseudo) · mv (pseudo)
"""),
    ref("syscalls", "Linux & Windows Syscall Quick Table", R, "Frequently used system calls", """
LINUX x86-64 (rax number)      LINUX ARM64 (x8)
0  read        1  write       63 read      64 write
2  open        3  close       56 openat    57 close
9  mmap       11  munmap     222 mmap     215 munmap
12 brk        39  getpid     214 brk      172 getpid
57 fork       59  execve     220 clone    221 execve
60 exit      231  exit_group  93 exit     94 exit_group
41 socket     42  connect    198 socket   203 connect
43 accept     44  sendto     202 accept   206 sendto
Inspect with:  strace -f ./app · ltrace · man 2 <name>

WINDOWS (use the Win32 / NT API, numbers change between builds)
CreateFile · ReadFile · WriteFile · CloseHandle · VirtualAlloc · CreateProcess · CreateThread · WaitForSingleObject · RegOpenKeyEx · WSASocket
Inspect with: Process Monitor · API Monitor · WinDbg
"""),
    ref("errors", "Error Codes: errno · exit codes · SQLSTATE · signals", R, "What the numbers mean", """
ERRNO (Linux)  1 EPERM · 2 ENOENT no such file · 5 EIO · 9 EBADF · 11 EAGAIN · 12 ENOMEM · 13 EACCES permission denied · 17 EEXIST · 20 ENOTDIR · 21 EISDIR · 22 EINVAL · 24 EMFILE too many files · 28 ENOSPC · 32 EPIPE · 98 EADDRINUSE · 104 ECONNRESET · 110 ETIMEDOUT · 111 ECONNREFUSED
EXIT CODES     0 success · 1 general error · 2 misuse of shell builtin · 126 not executable · 127 command not found · 128+N killed by signal N (130 = Ctrl+C, 137 = SIGKILL/OOM, 139 = SIGSEGV, 143 = SIGTERM)
SIGNALS        1 HUP · 2 INT · 3 QUIT · 6 ABRT · 9 KILL · 11 SEGV · 13 PIPE · 14 ALRM · 15 TERM · 17 CHLD
SQLSTATE       00000 ok · 23000 integrity violation · 23505 unique violation · 23503 FK violation · 22001 string too long · 22003 numeric overflow · 40001 serialization failure · 42P01 undefined table (PG) · 42601 syntax error · 08006 connection failure
HTTP→gRPC      200 OK · 400 INVALID_ARGUMENT · 401 UNAUTHENTICATED · 403 PERMISSION_DENIED · 404 NOT_FOUND · 409 ABORTED/ALREADY_EXISTS · 429 RESOURCE_EXHAUSTED · 500 INTERNAL · 503 UNAVAILABLE · 504 DEADLINE_EXCEEDED
JAVA           OutOfMemoryError · StackOverflowError · NullPointerException · ClassNotFoundException · ConcurrentModificationException
ANDROID        ANR (5 s main-thread block) · NetworkOnMainThreadException · SecurityException (missing permission) · ActivityNotFoundException
"""),
    ref("sql-dialects", "SQL Dialect Cheat Sheet", R, "Same task, different databases", """
Task                  MySQL                         PostgreSQL                    SQLite                        SQL Server
Limit rows            LIMIT 10                      LIMIT 10                      LIMIT 10                      SELECT TOP 10 / OFFSET-FETCH
Auto id               AUTO_INCREMENT                SERIAL / IDENTITY             INTEGER PRIMARY KEY           IDENTITY(1,1)
Now                   NOW()                         now()                         datetime('now')               GETDATE()
String concat         CONCAT(a,b)                   a || b                        a || b                        a + b / CONCAT
Upsert                ON DUPLICATE KEY UPDATE       ON CONFLICT … DO UPDATE       ON CONFLICT … DO UPDATE       MERGE
Boolean               TINYINT(1)                    BOOLEAN                       0/1                           BIT
Date diff             DATEDIFF(a,b)                 a - b                         julianday(a)-julianday(b)     DATEDIFF(day,a,b)
JSON                  JSON_EXTRACT                  ->> / jsonb                   json_extract                  JSON_VALUE
Regex                 REGEXP                        ~                             (extension)                   (LIKE / CLR)
Quote identifiers     `name`                        "name"                        "name"                        [name]
Explain               EXPLAIN                       EXPLAIN ANALYZE               EXPLAIN QUERY PLAN            SET SHOWPLAN_ALL ON
"""),
    ref("mermaid-ref", "Mermaid / UML / Flowchart Syntax", R, "Diagram-as-code templates (render with mermaid.live, GitHub, Notion, etc.)", """
FLOWCHART
flowchart TD
  A[Start] --> B{Valid?}
  B -- yes --> C[Process]
  B -- no --> D[Error]
  C --> E((End))

SEQUENCE
sequenceDiagram
  participant App
  participant API
  App->>API: POST /login
  API-->>App: 200 token

CLASS
classDiagram
  class Animal { +String name +eat() }
  Animal <|-- Dog
  Dog : +bark()

STATE
stateDiagram-v2
  [*] --> Idle
  Idle --> Loading : fetch
  Loading --> Success
  Loading --> Error

ER
erDiagram
  USER ||--o{ ORDER : places

GANTT
gantt
  title Plan
  dateFormat YYYY-MM-DD
  section Dev
  Build :a1, 2026-10-01, 10d

PlantUML equivalents: @startuml … @enduml
"""),
    ref("date-ref", "Date Format Cheat Sheet", R, "Same instant, every language", """
ISO 8601        2026-10-13T14:30:00Z   ·   2026-10-13T14:30:00+00:00   ·   week date 2026-W42-2
Java/Kotlin     yyyy-MM-dd'T'HH:mm:ssXXX      DateTimeFormatter.ISO_INSTANT
Python          %Y-%m-%dT%H:%M:%S%z           datetime.now(timezone.utc).isoformat()
JavaScript      new Date().toISOString()      Intl.DateTimeFormat("en-GB").format(d)
Moment/Day.js   YYYY-MM-DD[T]HH:mm:ssZ        (note: YYYY vs yyyy, DD vs dd differ from Java)
Go              2006-01-02T15:04:05Z07:00
C# / .NET       yyyy-MM-ddTHH:mm:ssK          DateTime.UtcNow.ToString("o")
PHP             Y-m-d\TH:i:sP                 date(DATE_ATOM)
Ruby            %Y-%m-%dT%H:%M:%S%:z          Time.now.utc.iso8601
Swift           ISO8601DateFormatter()        DateFormatter dateFormat "yyyy-MM-dd'T'HH:mm:ssZ"
SQL             CURRENT_TIMESTAMP · PG to_char(ts,'YYYY-MM-DD HH24:MI:SS') · MySQL DATE_FORMAT(ts,'%Y-%m-%d %H:%i:%s')
Pitfalls        always store UTC · beware DST gaps · leap seconds · Y2038 (32-bit time_t) · month is 0-based in JS Date
"""),
    ref("interview", "Interview Prep: Patterns & Questions", R, "Data structures, algorithms and system design checklists", """
PATTERNS → WHEN
Two pointers (sorted arrays, palindromes) · Sliding window (substrings, subarrays) · Fast/slow pointers (cycles) · Binary search (sorted or monotonic answer)
BFS (shortest path unweighted) · DFS/backtracking (permutations, subsets) · Heap (top-K) · Hash map (counting, lookup) · Stack (parentheses, next greater)
Union-find (connected components) · Topological sort (dependencies) · DP (overlapping subproblems: knapsack, LIS, edit distance) · Greedy (intervals) · Trie (prefix search)
CLASSIC QUESTIONS
Two Sum · Valid Parentheses · Merge Intervals · LRU Cache · Reverse Linked List · Binary Tree Level Order · Longest Substring Without Repeating · Coin Change · Number of Islands · Course Schedule · Word Ladder · Kth Largest · Trapping Rain Water
SYSTEM DESIGN CHECKLIST
Requirements (functional / non-functional) → estimates (QPS, storage) → API → data model → high-level design → deep dive (cache, queue, sharding, replication) → bottlenecks → trade-offs (CAP, consistency)
BEHAVIOURAL (STAR): Situation · Task · Action · Result — prepare 5 stories: conflict, failure, leadership, tight deadline, learning something new
"""),
    ref("challenges", "Coding Challenge Bank (offline)", R, "Practice problems with hints, easiest first", """
1  FizzBuzz            Print 1..100; multiples of 3 → Fizz, 5 → Buzz, both → FizzBuzz.                       hint: check 15 first
2  Palindrome          Is a string a palindrome ignoring case & punctuation?                                hint: two pointers
3  Two Sum             Indices of two numbers adding to a target.                                           hint: hash map of seen values
4  Valid Parentheses   Are ()[]{} balanced?                                                                 hint: stack
5  Anagram Groups      Group words that are anagrams.                                                       hint: sorted word as key
6  Reverse Words       Reverse word order in a sentence in place.                                           hint: reverse all, then each word
7  Max Subarray        Largest sum of a contiguous subarray.                                                hint: Kadane's algorithm
8  Merge Intervals     Merge overlapping [start, end] pairs.                                                hint: sort by start
9  Binary Search       Find index of x in sorted array in O(log n).                                         hint: mind the mid calculation
10 LRU Cache           get/put in O(1) with eviction.                                                       hint: hash map + doubly linked list
11 Longest Substring   Longest substring without repeating characters.                                     hint: sliding window
12 Roman ↔ Integer     Convert between Roman numerals and integers.                                         hint: subtractive pairs
13 Matrix Spiral       Return matrix elements in spiral order.                                              hint: shrinking boundaries
14 Min Coins           Fewest coins to make an amount.                                                      hint: DP over amounts
15 Number of Islands   Count connected groups of 1s in a grid.                                              hint: DFS/BFS flood fill
16 Top K Frequent      K most frequent elements.                                                            hint: map + min-heap or bucket sort
17 Detect Cycle        Does a linked list have a cycle?                                                     hint: slow & fast pointers
18 Serialize Tree      Encode / decode a binary tree to a string.                                           hint: pre-order with null markers
19 Word Search         Does a word exist in a letter grid?                                                  hint: backtracking
20 Median of Stream    Running median.                                                                      hint: two heaps
"""),
)
