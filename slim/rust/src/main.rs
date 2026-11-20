fn main() {
    let code = slim::status(slim::run(&std::env::args().collect::<Vec<_>>()));
    std::process::exit(code);
}
