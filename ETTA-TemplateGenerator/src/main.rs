use std::env;
use std::fs::{self, File};
use std::io::Write;
use std::path::PathBuf;

fn main() -> std::io::Result<()> {
    let args: Vec<String> = env::args().collect();

    if args.len() != 3 {
        eprintln!("Usage: ./{} <description> <item_path>", &args[0]);
        std::process::exit(1);
    }

    let description = &args[1]; // first argument
    let item_path = PathBuf::from(&args[2]);

    let item_name = item_path
        .file_name()
        .expect("invalid item path")
        .to_str()
        .unwrap();

    // Root directory = description (example used "abc")
    let root = PathBuf::from(description);

    let etta_dir = root
        .join(item_path.parent().unwrap())
        .join(format!("{item_name}.etta"));

    // Create directories
    fs::create_dir_all(etta_dir.join("frames"))?;

    // ---- pack.mcmeta ----
    let mut pack_file = File::create(root.join("pack.mcmeta"))?;
    write!(
        pack_file,
        r#"{{
  "pack": {{
    "pack_format": 64,
    "description": "{}"
  }}
}}
"#,
        description
    )?;

    // ---- diamond_sword.mcmetax ----
    let mut mcmetax = File::create(etta_dir.join(format!("{item_name}.mcmetax")))?;
    mcmetax.write_all(
        b"[animation]
frametime: 1

[fallback]
frame: 0
",
    )?;

    Ok(())
}
