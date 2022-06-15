export interface FPReaderPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
