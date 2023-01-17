ARG IMAGE=

FROM $IMAGE

# Dependencies for Rust
RUN apt-get update -y; \
    DEBIAN_FRONTEND=noninteractive apt-get install -y build-essential ca-certificates git make cmake wget unzip libtool automake jq && \
    rm -rf /var/lib/apt/lists/*

# https://rust-lang.github.io/rustup/installation/index.html#choosing-where-to-install
ENV RUSTUP_HOME=/usr/local/rustup
ENV CARGO_HOME=/usr/local/cargo
ENV PATH=/usr/local/cargo/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

# Install Rust
RUN curl https://sh.rustup.rs -sSf | sh -s -- -y

# Clone lddtopo-rs and build it
RUN git clone --depth 1 https://github.com/REASY/lddtopo-rs /opt/lddtopo-rs && cd /opt/lddtopo-rs && cargo build --release

COPY extract_native.sh /opt/

ENTRYPOINT ["/opt/extract_native.sh"]